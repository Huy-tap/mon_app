package com.example.fitnessapp.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object MediaStorage {
    fun exists(path: String?): Boolean = !path.isNullOrBlank() && File(path).isFile
    fun newFile(context: Context, extension: String): File = File(context.filesDir, "media/${UUID.randomUUID()}.$extension").apply { parentFile?.mkdirs() }
    fun import(context: Context, uri: Uri, video: Boolean): String {
        val type = context.contentResolver.getType(uri)
        val allowed = if (video) setOf("video/mp4") else setOf("image/jpeg", "image/png")
        require(type in allowed) { if (video) "Vui lòng chọn video MP4." else "Vui lòng chọn ảnh JPEG hoặc PNG." }
        val limit = (if (video) 100 else 10) * 1024L * 1024L
        val file = newFile(context, if (video) "mp4" else if (type == "image/png") "png" else "jpg")
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
        } catch (e: Exception) { file.delete(); throw e }
    }
}
