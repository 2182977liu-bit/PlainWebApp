package com.example.plainapp.server

import android.content.Context
import android.util.Log
import com.example.plainapp.browser.BrowserManager
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder

class BrowserHttpHandler(
    private val context: Context,
    private val browserManager: BrowserManager
) : NanoHTTPD("") {
    
    companion object {
        private const val TAG = "BrowserHttpHandler"
        private const val API_BROWSER = "/api/browser"
    }
    
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        Log.d(TAG, "Browser request: ${session.method} $uri")
        
        return when {
            uri.startsWith("$API_BROWSER/proxy") -> handleProxy(session)
            uri == "$API_BROWSER/download" -> handleDownload(session)
            uri == "$API_BROWSER/downloads" -> handleDownloads(session)
            uri == "$API_BROWSER/clearCache" -> handleClearCache(session)
            uri == "$API_BROWSER/settings" -> handleSettings(session)
            uri == "$API_BROWSER/progress" -> handleDownloadProgress(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
    }
    
    private fun handleProxy(session: IHTTPSession): Response {
        val params = session.parameters
        val urlParam = params["url"]?.firstOrNull() ?: return newFixedLengthResponse(
            Response.Status.BAD_REQUEST,
            "text/plain",
            "Missing url parameter"
        )
        
        val targetUrl = URLDecoder.decode(urlParam, "UTF-8")
        Log.d(TAG, "Proxying to: $targetUrl")
        
        return try {
            val result = browserManager.fetchUrl(targetUrl)
            
            if (result.success) {
                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    result.contentType,
                    result.content
                )
                response.addHeader("X-Frame-Options", "DENY")
                response.addHeader("X-Content-Type-Options", "nosniff")
                response.addHeader("X-Final-Url", result.finalUrl)
                response
            } else {
                newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "text/html",
                    "<html><body><h1>加载失败</h1><p>${escapeHtml(result.error)}</p></body></html>"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Proxy error", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/html",
                "<html><body><h1>代理错误</h1><p>${escapeHtml(e.message)}</p></body></html>"
            )
        }
    }
    
    private fun handleDownload(session: IHTTPSession): Response {
        if (session.method != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", 
                "{\"success\":false,\"error\":\"Method not allowed\"}")
        }
        
        return try {
            val bodyMap = HashMap<String, String>()
            session.parseBody(bodyMap)
            val postData = bodyMap["postData"] ?: ""
            
            val url = extractJsonValue(postData, "url") ?: return newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "application/json",
                "{\"success\":false,\"error\":\"Missing url\"}"
            )
            val filename = extractJsonValue(postData, "filename") ?: "download"
            val mimeType = extractJsonValue(postData, "mimeType")
            
            Log.d(TAG, "Download request: $url -> $filename")
            
            val result = browserManager.startDownload(url, filename, mimeType)
            
            val responseJson = if (result.success) {
                "{\"success\":true,\"filePath\":\"${escapeJson(result.filePath)}\",\"filename\":\"${escapeJson(result.filename)}\",\"size\":${result.size}}"
            } else {
                "{\"success\":false,\"error\":\"${escapeJson(result.error)}\"}"
            }
            
            newFixedLengthResponse(Response.Status.OK, "application/json", responseJson)
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                "{\"success\":false,\"error\":\"${escapeJson(e.message ?: "Unknown error")}\"}"
            )
        }
    }
    
    private fun handleDownloads(session: IHTTPSession): Response {
        return try {
            val downloads = browserManager.getDownloads()
            val json = downloads.joinToString(
                prefix = "[",
                postfix = "]",
                separator = ","
            ) { download ->
                """{"id":"${escapeJson(download.id)}","filename":"${escapeJson(download.filename)}","url":"${escapeJson(download.url)}","progress":${download.progress},"status":"${escapeJson(download.status)}","size":${download.size}}"""
            }
            
            val response = newFixedLengthResponse(Response.Status.OK, "application/json", json)
            response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
            response.addHeader("Pragma", "no-cache")
            response.addHeader("Expires", "0")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Get downloads error", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                "[]"
            )
        }
    }
    
    private fun handleDownloadProgress(session: IHTTPSession): Response {
        return try {
            val downloads = browserManager.getDownloads()
            val json = downloads.joinToString(
                prefix = "[",
                postfix = "]",
                separator = ","
            ) { download ->
                """{"id":"${escapeJson(download.id)}","filename":"${escapeJson(download.filename)}","progress":${download.progress},"status":"${escapeJson(download.status)}","size":${download.size},"filePath":"${escapeJson(download.filePath ?: "")}"}"""
            }
            
            val response = newFixedLengthResponse(Response.Status.OK, "application/json", json)
            response.addHeader("Cache-Control", "no-cache")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Progress error", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                "[]"
            )
        }
    }
    
    private fun handleClearCache(session: IHTTPSession): Response {
        return try {
            val success = browserManager.clearCache()
            val json = if (success) {
                "{\"success\":true}"
            } else {
                "{\"success\":false,\"error\":\"Clear cache failed\"}"
            }
            newFixedLengthResponse(Response.Status.OK, "application/json", json)
        } catch (e: Exception) {
            Log.e(TAG, "Clear cache error", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                "{\"success\":false,\"error\":\"${escapeJson(e.message ?: "Unknown error")}\"}"
            )
        }
    }
    
    private fun handleSettings(session: IHTTPSession): Response {
        return try {
            when (session.method) {
                Method.GET -> {
                    val settings = browserManager.getSettings()
                    val json = """{"homepage":"${escapeJson(settings.homepage)}","searchEngine":"${escapeJson(settings.searchEngine)}","downloadPath":"${escapeJson(settings.downloadPath)}","privateMode":${settings.privateMode}}"""
                    newFixedLengthResponse(Response.Status.OK, "application/json", json)
                }
                Method.POST -> {
                    val bodyMap = HashMap<String, String>()
                    session.parseBody(bodyMap)
                    val postData = bodyMap["postData"] ?: ""
                    
                    val settings = browserManager.getSettings()
                    extractJsonValue(postData, "homepage")?.let { settings.homepage = it }
                    extractJsonValue(postData, "searchEngine")?.let { settings.searchEngine = it }
                    extractJsonValue(postData, "downloadPath")?.let { settings.downloadPath = it }
                    extractJsonValue(postData, "privateMode")?.let { settings.privateMode = it.toBoolean() }
                    
                    val success = browserManager.saveSettings(settings)
                    val responseJson = if (success) {
                        "{\"success\":true}"
                    } else {
                        "{\"success\":false,\"error\":\"Save settings failed\"}"
                    }
                    newFixedLengthResponse(Response.Status.OK, "application/json", responseJson)
                }
                else -> newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", 
                    "{\"success\":false,\"error\":\"Method not allowed\"}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Settings error", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                "{\"success\":false,\"error\":\"${escapeJson(e.message ?: "Unknown error")}\"}"
            )
        }
    }
    
    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = """"$key"\s*:\s*"([^"]*)"""".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1)?.let {
            it.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
        }
    }
    
    private fun escapeHtml(text: String?): String {
        if (text == null) return ""
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }
    
    private fun escapeJson(text: String?): String {
        if (text == null) return ""
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}