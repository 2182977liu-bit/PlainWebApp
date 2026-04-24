package com.mobilenas.app.server.routing

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64

@Serializable
data class AppInfoItem(val packageName: String, val name: String, val versionName: String, val versionCode: Long, val iconBase64: String = "", val isSystem: Boolean, val sourceDir: String, val installTime: Long = 0)

fun Route.appRoutes(context: Context) {
    get("/apps") {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.sourceDir != null }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
            .map { info ->
                val name = info.loadLabel(pm).toString()
                val pi = try { pm.getPackageInfo(info.packageName, 0) } catch (_: Exception) { null }
                val versionName = pi?.versionName ?: ""
                val versionCode = if (pi != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()
                } else 0L
                val icon = try {
                    val d = info.loadIcon(pm)
                    if (d is BitmapDrawable) {
                        val s = ByteArrayOutputStream(); d.bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 64, s)
                        Base64.getEncoder().encodeToString(s.toByteArray())
                    } else ""
                } catch (_: Exception) { "" }
                val installTime = pi?.firstInstallTime ?: 0L
                AppInfoItem(info.packageName, name, versionName, versionCode, icon, (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0, info.sourceDir ?: "", installTime)
            }
        call.respond(apps)
    }

    get("/apps/export") {
        val pkg = call.request.queryParameters["package"] ?: return@get call.respondText("Missing package", status = HttpStatusCode.BadRequest)
        try {
            val ai = context.packageManager.getApplicationInfo(pkg, 0)
            val f = File(ai.sourceDir)
            if (!f.exists()) return@get call.respondText("APK not found", status = HttpStatusCode.NotFound)
            call.respondFile(f, ContentType.parse("application/vnd.android.package-archive"))
        } catch (e: Exception) {
            call.respondText("Error: ${e.message}", status = HttpStatusCode.NotFound)
        }
    }
}
