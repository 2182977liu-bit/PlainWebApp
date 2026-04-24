package com.mobilenas.app.server.routing

import android.content.Context
import android.os.Environment
import com.mobilenas.app.util.FileUtil
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class FileItem(val name: String, val path: String, val isDirectory: Boolean, val size: Long = 0, val lastModified: Long = 0, val mimeType: String = "")

@Serializable
data class FileListResponse(val path: String, val items: List<FileItem>, val parentPath: String?)

fun Route.fileRoutes(context: Context) {
    get("/files") {
        val path = call.request.queryParameters["path"] ?: "/sdcard"
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            call.respondText("""{"error":"Directory not found"}""", status = HttpStatusCode.NotFound)
            return@get
        }
        val files = dir.listFiles()?.sortedWith(compareByDescending<File> { it.isDirectory }.thenByDescending { it.lastModified() }) ?: emptyList()
        val items = files.map { f ->
            FileItem(name = f.name, path = f.absolutePath, isDirectory = f.isDirectory,
                size = if (f.isFile) f.length() else 0, lastModified = f.lastModified(),
                mimeType = if (f.isFile) FileUtil.getMimeType(f.name) else "")
        }
        val parent = if (path == "/sdcard" || path == "/") null else dir.parent?.absolutePath
        call.respond(FileListResponse(path, items, parent))
    }

    get("/files/download") {
        val path = call.request.queryParameters["path"] ?: return@get call.respondText("Missing path", status = HttpStatusCode.BadRequest)
        val file = File(path)
        if (!file.exists() || !file.isFile) return@get call.respondText("Not found", status = HttpStatusCode.NotFound)
        call.respondFile(file, ContentType.parse(FileUtil.getMimeType(file.name)))
    }

    post("/files/delete") {
        val params = call.receiveParameters()
        val paths = params["paths"]?.split(",") ?: return@post call.respondText("Missing paths", status = HttpStatusCode.BadRequest)
        val results = paths.associateWith { p -> val f = File(p); if (f.exists()) f.deleteRecursively() else false }
        call.respond(mapOf("results" to results))
    }

    post("/files/rename") {
        val params = call.receiveParameters()
        val path = params["path"] ?: return@post call.respondText("Missing path", status = HttpStatusCode.BadRequest)
        val newName = params["newName"] ?: return@post call.respondText("Missing newName", status = HttpStatusCode.BadRequest)
        val file = File(path)
        if (!file.exists()) return@post call.respondText("Not found", status = HttpStatusCode.NotFound)
        val newFile = File(file.parent, newName)
        call.respond(mapOf("success" to file.renameTo(newFile), "newPath" to newFile.absolutePath))
    }

    post("/files/mkdir") {
        val params = call.receiveParameters()
        val path = params["path"] ?: return@post call.respondText("Missing path", status = HttpStatusCode.BadRequest)
        val name = params["name"] ?: return@post call.respondText("Missing name", status = HttpStatusCode.BadRequest)
        val dir = File(path, name)
        call.respond(mapOf("success" to dir.mkdirs(), "fullPath" to dir.absolutePath))
    }

    get("/files/mounts") {
        val mounts = mutableListOf(mapOf("path" to Environment.getExternalStorageDirectory().absolutePath, "name" to "内部存储", "type" to "internal"))
        try {
            context.getExternalFilesDirs(null).forEach { dir ->
                if (dir != null) {
                    val p = dir.absolutePath.substringBefore("/Android")
                    if (p != Environment.getExternalStorageDirectory().absolutePath)
                        mounts.add(mapOf("path" to p, "name" to p.substringAfterLast("/"), "type" to "external"))
                }
            }
        } catch (_: Exception) {}
        call.respond(mounts)
    }
}
