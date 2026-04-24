package com.mobilenas.app.server

import android.content.Context
import android.util.Log
import com.mobilenas.app.data.repository.DownloadRepository
import com.mobilenas.app.data.repository.SettingsRepository
import com.mobilenas.app.util.FileUtil
import com.mobilenas.app.util.NetworkUtils
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration
import kotlin.concurrent.thread

class KtorServer(private val context: Context) {
    companion object {
        private const val TAG = "KtorServer"
        @Volatile
        private var instance: KtorServer? = null

        fun getInstance(): KtorServer? = instance

        fun start(context: Context) {
            if (instance != null) return
            instance = KtorServer(context)
            thread(name = "KtorServer") {
                instance!!.start()
            }
        }

        fun stop() {
            instance?.stop()
            instance = null
        }

        fun getServerUrl(): String {
            val inst = instance ?: return ""
            return "http://${NetworkUtils.getLocalIpAddress()}:${inst.currentPort}"
        }

        fun isRunning(): Boolean = instance?.isAlive() ?: false
    }

    private var server: NettyApplicationEngine? = null
    private var currentPort: Int = ServerConfig.DEFAULT_PORT

    val eventFlow = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 64)

    sealed class ServerEvent {
        data class DownloadProgress(val taskId: String, val progress: Int, val downloadedSize: Long, val totalSize: Long) : ServerEvent()
        data class DownloadCompleted(val taskId: String) : ServerEvent()
        data class DownloadFailed(val taskId: String, val error: String) : ServerEvent()
    }

    fun start() {
        val ports = listOf(ServerConfig.DEFAULT_PORT) + ServerConfig.FALLBACK_PORTS
        for (port in ports) {
            try {
                server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false })
                    }
                    install(CORS) {
                        anyHost()
                        allowHeader(HttpHeaders.ContentType)
                        allowHeader(HttpHeaders.Authorization)
                    }
                    install(Compression) {
                        gzip()
                    }
                    install(AutoHeadResponse)
                    install(ConditionalHeaders)
                    install(PartialContent)
                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(30)
                        timeout = Duration.ofSeconds(15)
                    }
                    routing {
                        staticFiles()
                        apiRoutes()
                        webSocketEvents()
                    }
                }.start(wait = false)
                currentPort = port
                Log.i(TAG, "Server started on port $port")
                return
            } catch (e: Exception) {
                Log.w(TAG, "Port $port unavailable: ${e.message}")
            }
        }
        Log.e(TAG, "Failed to start server on any port")
    }

    fun stop() {
        try {
            server?.stop(1000, 2000)
            Log.i(TAG, "Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    fun isAlive(): Boolean = try { (server as? NettyApplicationEngine)?.environment?.start() != null } catch (_: Exception) { false }

    private fun Routing.staticFiles() {
        get("/{path...}") {
            val path = call.parameters.getAll("path")?.joinToString("/") ?: ""
            serveStaticFile(call, path)
        }
    }

    private suspend fun serveStaticFile(call: ApplicationCall, path: String) {
        try {
            val assetPath = if (path.isEmpty() || path == "/") "web/index.html" else "web/$path"
            val inputStream = context.assets.open(assetPath)
            val bytes = ByteArrayOutputStream()
            inputStream.copyTo(bytes)
            inputStream.close()
            val content = bytes.toByteArray()
            val mimeType = FileUtil.getMimeType(path)
            call.respondBytes(content, ContentType.parse(mimeType))
        } catch (e: Exception) {
            try {
                val indexStream = context.assets.open("web/index.html")
                val bytes = ByteArrayOutputStream()
                indexStream.copyTo(bytes)
                indexStream.close()
                call.respondBytes(bytes.toByteArray(), ContentType.Text.Html)
            } catch (e2: Exception) {
                call.respondText("Not Found", status = HttpStatusCode.NotFound)
            }
        }
    }

    private fun Routing.apiRoutes() {
        route("/api") {
            downloadRoutes()
            settingsRoutes()
            browserRoutes()
            cacheRoutes()
        }
    }

    private fun Route.downloadRoutes() {
        val downloadRepo = DownloadRepository(
            (context.applicationContext as com.mobilenas.app.MobileNASApplication).database.downloadDao()
        )

        get("/downloads") {
            val downloads = downloadRepo.getAllDownloadsOnce()
            call.respond(downloads)
        }

        get("/downloads/progress") {
            val downloads = downloadRepo.getAllDownloadsOnce()
            call.respond(downloads.filter { it.status == "downloading" })
        }

        post("/downloads") {
            call.respond(mapOf("message" to "Use DownloadService to start downloads"))
        }
    }

    private fun Route.settingsRoutes() {
        val settingsRepo = SettingsRepository(
            (context.applicationContext as com.mobilenas.app.MobileNASApplication).database.appSettingsDao()
        )

        get("/settings") {
            val settings = settingsRepo.getAll()
            call.respond(settings)
        }

        put("/settings") {
            val params = call.receiveParameters()
            val key = params["key"] ?: return@put call.respondText("Missing key", status = HttpStatusCode.BadRequest)
            val value = params["value"] ?: ""
            settingsRepo.set(key, value)
            call.respond(mapOf("success" to true))
        }
    }

    private fun Route.browserRoutes() {
        get("/browser/proxy") {
            val url = call.request.queryParameters["url"]
                ?: return@get call.respondText("Missing url parameter", status = HttpStatusCode.BadRequest)
            call.respondText("Proxy endpoint - use DownloadService", status = HttpStatusCode.NotImplemented)
        }
    }

    private fun Route.cacheRoutes() {
        delete("/cache") {
            val browserCache = File(context.cacheDir, "browser_cache")
            if (browserCache.exists()) {
                browserCache.deleteRecursively()
            }
            call.respond(mapOf("success" to true))
        }
    }

    private fun Routing.webSocketEvents() {
        webSocket("/ws") {
            Log.i(TAG, "WebSocket client connected")
            try {
                launch {
                    eventFlow.collect { event ->
                        val json = when (event) {
                            is ServerEvent.DownloadProgress -> """{"type":"download_progress","taskId":"${event.taskId}","progress":${event.progress},"downloadedSize":${event.downloadedSize},"totalSize":${event.totalSize}}"""
                            is ServerEvent.DownloadCompleted -> """{"type":"download_completed","taskId":"${event.taskId}"}"""
                            is ServerEvent.DownloadFailed -> """{"type":"download_failed","taskId":"${event.taskId}","error":"${event.error}"}"""
                        }
                        outgoing.send(Frame.Text(json))
                    }
                }
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> Log.d(TAG, "WS received: ${frame.readText()}")
                        is Frame.Close -> {
                            Log.i(TAG, "WebSocket client disconnecting")
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client closed"))
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "WebSocket error", e)
            }
            Log.i(TAG, "WebSocket client disconnected")
        }
    }
}
