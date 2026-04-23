package com.example.plainapp.server

import android.content.Context
import android.util.Log
import com.example.plainapp.browser.BrowserManager
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

object ServerConfig {
    const val DEFAULT_PORT = 8080
    val FALLBACK_PORTS = listOf(8081, 8082, 8090, 3000)
    const val MAX_RETRY_ATTEMPTS = 5
}

class SimpleWebServer(
    private val appContext: Context,
    private val port: Int = ServerConfig.DEFAULT_PORT
) : NanoHTTPD(port) {
    
    companion object {
        private const val TAG = "SimpleWebServer"
        private var instance: SimpleWebServer? = null
        private var serverPort: Int = ServerConfig.DEFAULT_PORT
        
        fun start(context: Context, port: Int = ServerConfig.DEFAULT_PORT): SimpleWebServer? {
            return try {
                instance?.stop()
                
                val server = SimpleWebServer(context.applicationContext, port)
                server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                
                instance = server
                serverPort = port
                Log.i(TAG, "Server started on port $port")
                server
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start server on port $port: ${e.message}")
                
                if (port == ServerConfig.DEFAULT_PORT) {
                    return tryFallbackPorts(context)
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error starting server", e)
                null
            }
        }
        
        private fun tryFallbackPorts(context: Context): SimpleWebServer? {
            Log.w(TAG, "Trying fallback ports...")
            
            for (fallbackPort in ServerConfig.FALLBACK_PORTS) {
                try {
                    instance?.stop()

                    val server = SimpleWebServer(context.applicationContext, fallbackPort)
                    server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                    
                    instance = server
                    serverPort = fallbackPort
                    Log.i(TAG, "Server started on fallback port $fallbackPort")
                    return server
                } catch (e: IOException) {
                    Log.w(TAG, "Port $fallbackPort also unavailable: ${e.message}")
                }
            }
            
            Log.e(TAG, "All ports unavailable")
            return null
        }
        
        fun stop() {
            try {
                instance?.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping server", e)
            } finally {
                instance = null
                Log.i(TAG, "Server stopped")
            }
        }
        
        fun getLocalIpAddress(): String? {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (networkInterface in interfaces) {
                    if (!networkInterface.isUp) continue
                    
                    val addresses = Collections.list(networkInterface.inetAddresses)
                    for (address in addresses) {
                        if (!address.isLoopbackAddress && address is InetAddress) {
                            val hostAddress = address.hostAddress
                            if (hostAddress != null && hostAddress.indexOf(':') < 0) {
                                return hostAddress
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get IP address", e)
            }
            return null
        }
        
        fun getServerUrl(): String {
            val ip = getLocalIpAddress() ?: "localhost"
            return "http://$ip:${instance?.getListeningPort() ?: serverPort}"
        }
        
        fun isRunning(): Boolean = instance?.wasStarted() == true
    }
    
    private val browserManager = BrowserManager(appContext)
    private val browserHttpHandler = BrowserHttpHandler(appContext, browserManager)
    
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        Log.d(TAG, "Request: ${session.method} $uri")
        
        return try {
            when {
                uri.startsWith("/api/browser") -> browserHttpHandler.serve(session)
                uri == "/browser.html" -> serveBrowserPage(session)
                uri == "/" || uri == "/index.html" -> serveIndexPage(session)
                else -> serveStaticResource(session)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled error serving $uri", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/html",
                "<html><body><h1>服务器错误</h1><p>处理请求时发生错误</p></body></html>"
            )
        }
    }
    
    private fun serveBrowserPage(session: IHTTPSession): Response {
        val browserPage = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta http-equiv="refresh" content="0;url=/browser.html">
                <title>浏览器 - PlainApp</title>
            </head>
            <body>
                <h1>PlainApp 浏览器</h1>
                <p>浏览器功能正在加载...</p>
            </body>
            </html>
        """.trimIndent()
        
        return newFixedLengthResponse(Response.Status.OK, "text/html", browserPage)
    }
    
    private fun serveIndexPage(session: IHTTPSession): Response {
        val indexPage = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>PlainApp</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    .menu { list-style: none; padding: 0; }
                    .menu li { margin: 10px 0; }
                    .menu a { 
                        display: block; 
                        padding: 15px 20px; 
                        background: #2196F3; 
                        color: white; 
                        text-decoration: none; 
                        border-radius: 8px;
                    }
                    .menu a:hover { background: #1976D2; }
                </style>
            </head>
            <body>
                <h1>PlainApp</h1>
                <ul class="menu">
                    <li><a href="/browser.html">🌐 浏览器</a></li>
                </ul>
            </body>
            </html>
        """.trimIndent()
        
        return newFixedLengthResponse(Response.Status.OK, "text/html", indexPage)
    }
    
    private fun serveStaticResource(session: IHTTPSession): Response {
        val uri = session.uri.trimStart('/')
        
        return try {
            val mimeType = getMimeType(uri)
            
            val assetPaths = listOf(
                "assets/$uri",
                "assets/web/$uri",
                "web/$uri"
            )
            
            var content: String? = null
            for (assetPath in assetPaths) {
                try {
                    val inputStream = appContext.assets.open(assetPath)
                    content = inputStream.bufferedReader().use { it.readText() }
                    break
                } catch (e: IOException) {
                    // Try next path
                }
            }
            
            if (content != null) {
                val response = newFixedLengthResponse(Response.Status.OK, mimeType, content)
                response.addHeader("Cache-Control", "public, max-age=3600")
                response
            } else {
                newFixedLengthResponse(
                    Response.Status.NOT_FOUND, 
                    "text/plain", 
                    "资源未找到: $uri"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error serving resource: $uri", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, 
                "text/plain", 
                "加载资源时发生错误"
            )
        }
    }
    
    private fun getMimeType(uri: String): String {
        return when {
            uri.endsWith(".html") || uri.endsWith(".htm") -> "text/html; charset=utf-8"
            uri.endsWith(".css") -> "text/css; charset=utf-8"
            uri.endsWith(".js") -> "application/javascript; charset=utf-8"
            uri.endsWith(".json") -> "application/json; charset=utf-8"
            uri.endsWith(".png") -> "image/png"
            uri.endsWith(".jpg") || uri.endsWith(".jpeg") -> "image/jpeg"
            uri.endsWith(".gif") -> "image/gif"
            uri.endsWith(".svg") -> "image/svg+xml"
            uri.endsWith(".ico") -> "image/x-icon"
            uri.endsWith(".pdf") -> "application/pdf"
            uri.endsWith(".zip") -> "application/zip"
            uri.endsWith(".mp3") -> "audio/mpeg"
            uri.endsWith(".mp4") -> "video/mp4"
            uri.endsWith(".webp") -> "image/webp"
            uri.endsWith(".woff") -> "font/woff"
            uri.endsWith(".woff2") -> "font/woff2"
            uri.endsWith(".ttf") -> "font/ttf"
            else -> "application/octet-stream"
        }
    }
    
}