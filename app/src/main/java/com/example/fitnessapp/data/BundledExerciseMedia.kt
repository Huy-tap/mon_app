package com.example.fitnessapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import android.system.Os
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Đọc đường dẫn media/... trong database đóng gói, gắn media theo exercise_id.
 * Database trên thiết bị giữ đường dẫn file đã sao chép; không thay database người dùng.
 */
object BundledExerciseMedia {
    internal data class Entry(val exerciseId: Long, val column: String, val asset: String)
    private const val TAG = "BundledExerciseMedia"

    fun install(context: Context, database: SQLiteDatabase) {
        // Đọc bản đi kèm APK để app đã cài cũng nhận được media mẫu mới.
        // Tệp tạm được đóng/xóa ngay sau khi đọc, không thay database đang dùng.
        var temporary: File? = null
        try {
            temporary = File.createTempFile("bundled-exercises-", ".db", context.cacheDir)
            context.assets.open("fitness_app.db").use { input ->
                temporary.outputStream().use { input.copyTo(it) }
            }
            val entries = SQLiteDatabase.openDatabase(temporary.path, null, SQLiteDatabase.OPEN_READONLY).use { seed ->
                seed.rawQuery("SELECT exercise_id, instruction_image, instruction_video FROM exercises", null).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            listOf("instruction_image", "instruction_video").forEachIndexed { index, column ->
                                val path = cursor.getString(index + 1)
                                if (!path.isNullOrBlank()) add(Entry(cursor.getLong(0), column, path))
                            }
                        }
                    }
                }
            }
            installEntries(context, database, entries)
        } catch (e: Exception) {
            // Sai/mất media mẫu không được chặn mở danh mục hoặc mất dữ liệu người dùng.
            Log.w(TAG, "Không đọc được media mẫu từ database đóng gói", e)
        } finally { temporary?.delete() }
    }

    internal fun installEntries(context: Context, database: SQLiteDatabase, entries: List<Entry>) {
        val apkVersion = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime.toString()
        entries.forEach { entry ->
            try { installEntry(context, database, entry, apkVersion) }
            catch (e: Exception) { Log.w(TAG, "Chưa cài được media mẫu: bài ${entry.exerciseId}, ${entry.asset}", e) }
        }
    }

    private fun installEntry(context: Context, database: SQLiteDatabase, entry: Entry, apkVersion: String) {
        require(entry.column in setOf("instruction_image", "instruction_video"))
        val video = entry.column == "instruction_video"
        val prefix = if (video) "media/videos/" else "media/images/"
        // Chỉ chấp nhận đường dẫn bên trong assets/media, không nhận đường dẫn máy tính.
        require(entry.asset.startsWith(prefix) && entry.asset.split('/').all { it.isNotBlank() && it != "." && it != ".." } &&
            !entry.asset.contains('\\') && !entry.asset.contains(':')) { "Đường dẫn media mẫu không hợp lệ" }
        val current = database.rawQuery(
            "SELECT ${entry.column} FROM exercises WHERE exercise_id = ? AND is_archived = 0",
            arrayOf(entry.exerciseId.toString())
        ).use { if (!it.moveToFirst()) return else it.getString(0) }
        val key = "bundled_media:${entry.exerciseId}:${entry.column}"
        val previous = database.rawQuery("SELECT state_value FROM app_state WHERE state_key = ?", arrayOf(key)).use {
            if (it.moveToFirst()) JSONObject(it.getString(0)) else null
        }
        val previousPath = previous?.optString("installedPath")?.takeIf { it.isNotBlank() }
        // Đường dẫn /uploads/... là đường dẫn mẫu cũ; không dùng làm file trên Android.
        val oldSeedPath = "/uploads/" + entry.asset.removePrefix("media/")
        val mayInstall = if (previous == null) {
            current.isNullOrBlank() || current == entry.asset || current == oldSeedPath
        } else {
            previousPath != null && current == previousPath
        }
        if (!mayInstall) {
            // Ghi nhận đã xét trường này: sau khi người dùng xóa media, không tự gắn lại.
            if (previous == null) remember(database, key, JSONObject().put("source", entry.asset))
            return
        }
        val directory = context.getExternalFilesDir(if (video) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES)
            ?: error("Bộ nhớ media chưa sẵn sàng")
        val destination = File(directory, "bundled/exercise_${entry.exerciseId}/${entry.asset.substringAfterLast('/')}")
        val upToDate = previous?.optString("source") == entry.asset &&
            previous?.optString("apkVersion") == apkVersion &&
            current == destination.absolutePath && destination.isFile && destination.length() > 0
        if (upToDate) return
        copyAsset(context, entry.asset, destination)

        database.beginTransaction()
        try {
            // Chỉ cập nhật trường đang xử lý, giữ tên, cấu hình, lịch sử và trường media kia.
            val values = ContentValues().apply { put(entry.column, destination.absolutePath) }
            val updated = database.update("exercises", values,
                "exercise_id = ? AND is_archived = 0 AND ${entry.column} IS ?",
                arrayOf(entry.exerciseId.toString(), current))
            if (updated > 0) remember(database, key, JSONObject()
                .put("source", entry.asset).put("installedPath", destination.absolutePath).put("apkVersion", apkVersion))
            database.setTransactionSuccessful()
        } finally { database.endTransaction() }
    }

    private fun copyAsset(context: Context, asset: String, destination: File) {
        check(destination.parentFile?.mkdirs() == true || destination.parentFile?.isDirectory == true)
        val temporary = File.createTempFile("copy-", ".tmp", destination.parentFile)
        try {
            context.assets.open(asset).use { input ->
                temporary.outputStream().use { output ->
                    check(input.copyTo(output) > 0) { "File media mẫu rỗng" }
                    output.fd.sync()
                }
            }
            // Đổi tên nguyên tử: ứng dụng không đọc phải file đang sao chép dở.
            Os.rename(temporary.absolutePath, destination.absolutePath)
        } finally { temporary.delete() }
    }

    private fun remember(database: SQLiteDatabase, key: String, value: JSONObject) {
        val values = ContentValues().apply {
            put("state_key", key); put("state_value", value.toString())
        }
        check(database.insertWithOnConflict("app_state", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L)
    }
}
