package com.mobilenas.app.util

import java.io.File

object FileUtil {
    private val MIME_TYPES = mapOf(
        "html" to "text/html", "htm" to "text/html", "css" to "text/css",
        "js" to "application/javascript", "json" to "application/json",
        "png" to "image/png", "jpg" to "image/jpeg", "jpeg" to "image/jpeg",
        "gif" to "image/gif", "svg" to "image/svg+xml", "ico" to "image/x-icon",
        "mp4" to "video/mp4", "webm" to "video/webm", "mp3" to "audio/mpeg",
        "wav" to "audio/wav", "ogg" to "audio/ogg", "pdf" to "application/pdf",
        "zip" to "application/zip", "apk" to "application/vnd.android.package-archive",
        "txt" to "text/plain", "xml" to "application/xml",
    )

    fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MIME_TYPES[ext] ?: "application/octet-stream"
    }

    fun formatFileSize(size: Long): String {
        if (size < 1024) return "$size B"
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0)
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024))
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024))
    }

    fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .take(255)
    }
}
