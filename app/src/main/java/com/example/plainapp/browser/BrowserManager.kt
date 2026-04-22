package com.example.plainapp.browser

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class BrowserManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BrowserManager"
        private const val CACHE_DIR = "browser_cache"
        private const val DOWNLOADS_DIR = "browser_downloads"
        private const val SETTINGS_FILE = "browser_settings.json"
        private const val MAX_CACHE_SIZE = 50 * 1024 * 1024L
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    private val downloads = mutableListOf<DownloadTask>()
    private var downloadTaskCounter = 0
    
    data class DownloadTask(
        val id: String,
        val url: String,
        val filename: String,
        val mimeType: String?,
        val size: Long,
        @Volatile var progress: Int,
        @Volatile var status: String,
        @Volatile var filePath: String?
    )
    
    data class BrowserSettings(
        var homepage: String = "https://www.baidu.com",
        var searchEngine: String = "https://www.baidu.com/s?wd=",
        var downloadPath: String = "/storage/emulated/0/Download",
        var privateMode: Boolean = false,
        var userAgent: String = "Mozilla/5.0 (Linux; Android 10; Pixel 3 XL) AppleWebKit/537.36"
    )
    
    suspend fun fetchUrl(targetUrl: String): BrowserResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching URL: $targetUrl")
            
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", getSettings().userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            val contentType = response.header("Content-Type", "text/html")
            val body = response.body?.string() ?: ""
            
            val processedHtml = processHtmlContent(body, targetUrl)
            
            BrowserResult(
                success = true,
                content = processedHtml,
                contentType = contentType ?: "text/html",
                finalUrl = response.request.url.toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch URL", e)
            BrowserResult(
                success = false,
                error = e.message ?: "Unknown error",
                contentType = "text/html"
            )
        }
    }
    
    private fun processHtmlContent(html: String, baseUrl: String): String {
        var processedHtml = html
        
        processedHtml = fixRelativeUrls(processedHtml, baseUrl)
        
        processedHtml = addSecurityHeaders(processedHtml)
        
        processedHtml = injectDownloadScript(processedHtml)
        
        return processedHtml
    }
    
    private fun fixRelativeUrls(html: String, baseUrl: String): String {
        var processedHtml = html
        val baseUri = android.net.Uri.parse(baseUrl)
        val baseHost = baseUri.scheme + "://" + baseUri.host
        
        val urlPatterns = listOf(
            Pattern.compile("href=\"(/[^\"']*)\""),
            Pattern.compile("src=\"(/[^\"']*)\""),
            Pattern.compile("href='(/[^\"']*)'"),
            Pattern.compile("src='(/[^\"']*)'")
        )
        
        val skipProtocols = listOf("javascript:", "mailto:", "tel:", "data:", "blob:", "#")
        
        for (pattern in urlPatterns) {
            val matcher = pattern.matcher(processedHtml)
            val buffer = StringBuffer()
            while (matcher.find()) {
                val fullMatch = matcher.group(0)
                val relativeUrl = matcher.group(1)
                
                val shouldSkip = skipProtocols.any { relativeUrl.startsWith(it) }
                
                if (relativeUrl.startsWith("/") && !shouldSkip) {
                    val absoluteUrl = baseHost + relativeUrl
                    matcher.appendReplacement(buffer, "")
                    buffer.append(fullMatch.replace(relativeUrl, absoluteUrl))
                } else {
                    matcher.appendReplacement(buffer, "")
                    buffer.append(fullMatch)
                }
            }
            matcher.appendTail(buffer)
            processedHtml = buffer.toString()
        }
        
        return processedHtml
    }
    
    private fun addSecurityHeaders(html: String): String {
        val securityHeaders = """
            <meta http-equiv="Content-Security-Policy" content="default-src 'self' https: data: blob:; script-src 'self' 'unsafe-inline' https:; style-src 'self' 'unsafe-inline' https:; img-src 'self' data: blob: https:; media-src 'self' data: blob: https:; frame-src 'self' https:; connect-src 'self' https:;">
            <meta http-equiv="X-Frame-Options" content="DENY">
            <meta http-equiv="X-Content-Type-Options" content="nosniff">
            <meta http-equiv="Referrer-Policy" content="strict-origin-when-cross-origin">
        """.trimIndent()
        
        return html.replace("<head>", "<head>\n$securityHeaders")
    }
    
    private fun injectDownloadScript(html: String): String {
        val downloadScript = """
            <script>
                window.addEventListener('beforeunload', function(e) {
                    var downloads = document.querySelectorAll('a[href], area[href]');
                    downloads.forEach(function(link) {
                        var href = link.getAttribute('href');
                        if (href && (href.endsWith('.pdf') || href.endsWith('.apk') || 
                            href.endsWith('.zip') || href.endsWith('.jpg') || 
                            href.endsWith('.png') || href.endsWith('.mp3') ||
                            href.endsWith('.mp4') || href.endsWith('.doc'))) {
                            link.setAttribute('download', href.split('/').pop());
                        }
                    });
                });
                
                document.addEventListener('click', function(e) {
                    var target = e.target;
                    while (target && target.tagName !== 'A') {
                        target = target.parentElement;
                    }
                    if (target && target.href) {
                        var href = target.href;
                        if (href && !href.startsWith('javascript:')) {
                            e.preventDefault();
                            var filename = href.split('/').pop() || 'download';
                            window.parent.postMessage({
                                type: 'download',
                                url: href,
                                filename: filename,
                                mimeType: target.getAttribute('download') || 'application/octet-stream'
                            }, '*');
                        }
                    }
                });
            </script>
        """.trimIndent()
        
        return html.replace("</body>", "$downloadScript\n</body>")
    }
    
    data class BrowserResult(
        val success: Boolean,
        val content: String = "",
        val contentType: String = "text/html",
        val finalUrl: String = "",
        val error: String = ""
    )
    
    suspend fun startDownload(url: String, filename: String, mimeType: String?): DownloadResult = withContext(Dispatchers.IO) {
        val taskId = "dl_${++downloadTaskCounter}"
        val sanitizedFilename = filename.replace(Regex("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]"), "_")
        
        val downloadTask = DownloadTask(
            id = taskId,
            url = url,
            filename = sanitizedFilename,
            mimeType = mimeType,
            size = 0,
            progress = 0,
            status = "等待中",
            filePath = null
        )
        
        synchronized(downloads) {
            downloads.add(0, downloadTask)
        }
        
        try {
            val downloadDir = File(context.getExternalFilesDir(null), DOWNLOADS_DIR)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            val file = File(downloadDir, sanitizedFilename)
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", getSettings().userAgent)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            
            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            
            synchronized(downloads) {
                val index = downloads.indexOfFirst { it.id == taskId }
                if (index >= 0) {
                    downloads[index] = downloads[index].copy(
                        size = if (totalBytes > 0) totalBytes else 0,
                        status = "下载中"
                    )
                }
            }
            
            FileOutputStream(file).use { outputStream ->
                body.byteStream().use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else {
                            0
                        }
                        
                        synchronized(downloads) {
                            val index = downloads.indexOfFirst { it.id == taskId }
                            if (index >= 0) {
                                downloads[index] = downloads[index].copy(progress = progress)
                            }
                        }
                    }
                }
            }
            
            synchronized(downloads) {
                val index = downloads.indexOfFirst { it.id == taskId }
                if (index >= 0) {
                    downloads[index] = downloads[index].copy(
                        progress = 100,
                        status = "已完成",
                        filePath = file.absolutePath
                    )
                }
            }
            
            DownloadResult(
                success = true,
                filePath = file.absolutePath,
                filename = sanitizedFilename,
                size = file.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            
            synchronized(downloads) {
                val index = downloads.indexOfFirst { it.id == taskId }
                if (index >= 0) {
                    downloads[index] = downloads[index].copy(status = "失败: ${e.message}")
                }
            }
            
            DownloadResult(
                success = false,
                error = e.message ?: "Download failed"
            )
        }
    }
    
    data class DownloadResult(
        val success: Boolean,
        val filePath: String = "",
        val filename: String = "",
        val size: Long = 0,
        val error: String = ""
    )
    
    fun getDownloads(): List<DownloadTask> {
        return synchronized(downloads) {
            downloads.toList()
        }
    }
    
    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
            false
        }
    }
    
    fun getSettings(): BrowserSettings {
        return try {
            val settingsFile = File(context.filesDir, SETTINGS_FILE)
            if (settingsFile.exists()) {
                val json = settingsFile.readText()
                parseSettings(json)
            } else {
                BrowserSettings()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load settings", e)
            BrowserSettings()
        }
    }
    
    fun saveSettings(settings: BrowserSettings): Boolean {
        return try {
            val settingsFile = File(context.filesDir, SETTINGS_FILE)
            val json = serializeSettings(settings)
            settingsFile.writeText(json)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings", e)
            false
        }
    }
    
    private fun parseSettings(json: String): BrowserSettings {
        return try {
            val settings = BrowserSettings()
            
            if (json.contains("{")) {
                val homepageMatch = Regex("\"homepage\"\\s*:\\s*\"([^\"]*)\"").find(json)
                val searchEngineMatch = Regex("\"searchEngine\"\\s*:\\s*\"([^\"]*)\"").find(json)
                val downloadPathMatch = Regex("\"downloadPath\"\\s*:\\s*\"([^\"]*)\"").find(json)
                val privateModeMatch = Regex("\"privateMode\"\\s*:\\s*(true|false)").find(json)
                val userAgentMatch = Regex("\"userAgent\"\\s*:\\s*\"([^\"]*)\"").find(json)
                
                homepageMatch?.groupValues?.get(1)?.let { settings.homepage = it }
                searchEngineMatch?.groupValues?.get(1)?.let { settings.searchEngine = it }
                downloadPathMatch?.groupValues?.get(1)?.let { settings.downloadPath = it }
                privateModeMatch?.groupValues?.get(1)?.let { settings.privateMode = it.toBoolean() }
                userAgentMatch?.groupValues?.get(1)?.let { settings.userAgent = it }
            } else {
                val map = json.split(";")
                    .map { it.split("=") }
                    .filter { it.size == 2 }
                    .associate { it[0].trim() to it[1].trim() }
                
                settings.homepage = map["homepage"] ?: settings.homepage
                settings.searchEngine = map["searchEngine"] ?: settings.searchEngine
                settings.downloadPath = map["downloadPath"] ?: settings.downloadPath
                settings.privateMode = map["privateMode"]?.toBoolean() ?: false
                settings.userAgent = URLDecoder.decode(map["userAgent"] ?: settings.userAgent, "UTF-8")
            }
            
            settings
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse settings", e)
            BrowserSettings()
        }
    }
    
    private fun serializeSettings(settings: BrowserSettings): String {
        return buildString {
            append("{")
            append("\"homepage\":\"${escapeJsonString(settings.homepage)}\",")
            append("\"searchEngine\":\"${escapeJsonString(settings.searchEngine)}\",")
            append("\"downloadPath\":\"${escapeJsonString(settings.downloadPath)}\",")
            append("\"privateMode\":${settings.privateMode},")
            append("\"userAgent\":\"${escapeJsonString(settings.userAgent)}\"")
            append("}")
        }
    }
    
    private fun escapeJsonString(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}