package com.example.fitnessapp.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.media.MediaMetadataRetriever
import java.io.File
import java.util.UUID

object MediaStorage {
    fun exists(path: String?): Boolean = !path.isNullOrBlank() && File(path).isFile
    fun newFile(context: Context, extension: String, displayName: String? = null): File {
        val directory = context.getExternalFilesDir(if (extension == "mp4") Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES)
        requireNotNull(directory) { "Không thể truy cập bộ nhớ để lưu ảnh/video." }
        val safeName = displayName?.substringAfterLast('/')?.substringAfterLast('\\')
            ?.replace(Regex("[^\\p{L}\\p{N} ._()-]"), "_")?.take(100)
            ?.takeIf { it.isNotBlank() && it != "." && it != ".." }
        return File(File(directory, UUID.randomUUID().toString()), safeName ?: "${if (extension == "mp4") "video" else "anh"}.$extension")
            .apply { check(parentFile?.mkdirs() == true || parentFile?.isDirectory == true) { "Không tạo được thư mục media." } }
    }
    fun import(context: Context, uri: Uri, video: Boolean): String {
        val type = context.contentResolver.getType(uri)
        val allowed = if (video) setOf("video/mp4") else setOf("image/jpeg", "image/png")
        require(type in allowed) { if (video) "Vui lòng chọn video MP4." else "Vui lòng chọn ảnh JPEG hoặc PNG." }
        val limit = (if (video) 100 else 10) * 1024L * 1024L
        val name = runCatching { context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } }.getOrNull()
        val file = newFile(context, if (video) "mp4" else if (type == "image/png") "png" else "jpg", name)
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Không đọc được tệp đã chọn." }
                file.outputStream().use { output ->
                    val buffer = ByteArray(8192); var total = 0L
                    while (true) { val n = input.read(buffer); if (n < 0) break
                        total += n; require(total <= limit) { "Tệp vượt giới hạn ${if (video) 100 else 10} MiB." }; output.write(buffer, 0, n)
                    }
                    require(total > 0) { "Tệp đã chọn trống." }
                }
            }
            return file.absolutePath
        } catch (e: Exception) { file.delete(); file.parentFile?.delete(); throw e }
    }
    fun videoDuration(path: String?): String? {
        if (!exists(path)) return null
        val metadata = MediaMetadataRetriever()
        return try {
            metadata.setDataSource(path)
            metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                val seconds = it / 1000
                "%02d:%02d".format(seconds / 60, seconds % 60)
            }
        } catch (_: Exception) { null } finally { runCatching { metadata.release() } }
    }
}
