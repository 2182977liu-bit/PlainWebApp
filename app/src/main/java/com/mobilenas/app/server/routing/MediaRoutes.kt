package com.mobilenas.app.server.routing

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.mobilenas.app.util.FileUtil
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class MediaItem(val id: Long, val name: String, val path: String, val size: Long, val mimeType: String, val dateAdded: Long, val uri: String = "")

@Serializable
data class MediaListResponse(val items: List<MediaItem>, val total: Int, val page: Int, val pageSize: Int)

fun Route.mediaRoutes(context: Context) {
    get("/media/images") {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50
        val offset = (page - 1) * pageSize
        val items = mutableListOf<MediaItem>()
        val proj = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.DATE_ADDED)
        context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { c ->
            val total = c.count
            if (c.moveToPosition(offset)) { var count = 0
                while (count < pageSize && !c.isAfterLast) {
                    val id = c.getLong(0); val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    items.add(MediaItem(id, c.getString(1) ?: "", c.getString(2) ?: "", c.getLong(3), c.getString(4) ?: "image/*", c.getLong(5), uri.toString()))
                    count++; c.moveToNext()
                }
            }
            call.respond(MediaListResponse(items, total, page, pageSize))
        } ?: call.respond(MediaListResponse(emptyList(), 0, page, pageSize))
    }

    get("/media/videos") {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50
        val offset = (page - 1) * pageSize
        val items = mutableListOf<MediaItem>()
        val proj = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DATA, MediaStore.Video.Media.SIZE, MediaStore.Video.Media.MIME_TYPE, MediaStore.Video.Media.DATE_ADDED)
        context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC")?.use { c ->
            val total = c.count
            if (c.moveToPosition(offset)) { var count = 0
                while (count < pageSize && !c.isAfterLast) {
                    val id = c.getLong(0); val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    items.add(MediaItem(id, c.getString(1) ?: "", c.getString(2) ?: "", c.getLong(3), c.getString(4) ?: "video/*", c.getLong(5), uri.toString()))
                    count++; c.moveToNext()
                }
            }
            call.respond(MediaListResponse(items, total, page, pageSize))
        } ?: call.respond(MediaListResponse(emptyList(), 0, page, pageSize))
    }

    get("/media/audio") {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50
        val offset = (page - 1) * pageSize
        val items = mutableListOf<MediaItem>()
        val proj = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.DATE_ADDED)
        context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, "${MediaStore.Audio.Media.DATE_ADDED} DESC")?.use { c ->
            val total = c.count
            if (c.moveToPosition(offset)) { var count = 0
                while (count < pageSize && !c.isAfterLast) {
                    val id = c.getLong(0); val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    items.add(MediaItem(id, c.getString(1) ?: "", c.getString(2) ?: "", c.getLong(3), c.getString(4) ?: "audio/*", c.getLong(5), uri.toString()))
                    count++; c.moveToNext()
                }
            }
            call.respond(MediaListResponse(items, total, page, pageSize))
        } ?: call.respond(MediaListResponse(emptyList(), 0, page, pageSize))
    }

    get("/media/file") {
        val path = call.request.queryParameters["path"] ?: return@get call.respondText("Missing path", status = HttpStatusCode.BadRequest)
        val file = File(path)
        if (!file.exists()) return@get call.respondText("Not found", status = HttpStatusCode.NotFound)
        call.respondFile(file, ContentType.parse(FileUtil.getMimeType(file.name)))
    }
}
