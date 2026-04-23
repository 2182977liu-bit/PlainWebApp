package com.mobilenas.app.service

import android.content.Context
import android.util.Log
import com.mobilenas.app.data.db.entity.DownloadEntity
import com.mobilenas.app.data.repository.DownloadRepository
import com.mobilenas.app.server.KtorServer
import com.mobilenas.app.util.FileUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.UUID
import java.util.concurrent.TimeUnit

class DownloadService(private val context: Context) {
    companion object {
        private const val TAG = "DownloadService"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val downloadDir = File(context.getExternalFilesDir(null), "downloads").apply { mkdirs() }

    fun startDownload(url: String, filename: String = "", mimeType: String = "") {
        scope.launch {
            try {
                val taskId = UUID.randomUUID().toString()
                val cleanFilename = if (filename.isNotEmpty()) FileUtil.sanitizeFileName(filename)
                    else FileUtil.sanitizeFileName(url.substringAfterLast("/").substringBefore("?")).ifEmpty { "download_${System.currentTimeMillis()}" }

                val entity = DownloadEntity(
                    taskId = taskId,
                    url = url,
                    filename = cleanFilename,
                    mimeType = mimeType,
                    status = "downloading"
                )
                val repo = DownloadRepository(
                    (context.applicationContext as com.mobilenas.app.MobileNASApplication).database.downloadDao()
                )
                repo.insert(entity)

                val request = Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    repo.update(entity.copy(status = "failed"))
                    KtorServer.getInstance()?.eventFlow?.emit(KtorServer.ServerEvent.DownloadFailed(taskId, "HTTP ${response.code}"))
                    return@launch
                }

                val responseBody = response.body ?: return@launch
                val totalSize = responseBody.contentLength()
                val outputFile = File(downloadDir, cleanFilename)
                var downloadedSize = 0L

                responseBody.byteStream().use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedSize += bytesRead
                            val progress = if (totalSize > 0) ((downloadedSize * 100) / totalSize).toInt() else -1
                            repo.update(entity.copy(downloadedSize = downloadedSize, totalSize = totalSize, progress = progress))
                            KtorServer.getInstance()?.eventFlow?.emit(
                                KtorServer.ServerEvent.DownloadProgress(taskId, progress, downloadedSize, totalSize)
                            )
                        }
                    }
                }

                repo.update(entity.copy(status = "completed", downloadedSize = downloadedSize, totalSize = totalSize, filePath = outputFile.absolutePath, progress = 100))
                KtorServer.getInstance()?.eventFlow?.emit(KtorServer.ServerEvent.DownloadCompleted(taskId))
                Log.i(TAG, "Download completed: $cleanFilename")
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                KtorServer.getInstance()?.eventFlow?.emit(KtorServer.ServerEvent.DownloadFailed(UUID.randomUUID().toString(), e.message ?: "Unknown error"))
            }
        }
    }
}
