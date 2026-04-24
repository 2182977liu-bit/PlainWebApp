package com.mobilenas.app.server.routing

import android.content.Context
import com.mobilenas.app.util.DeviceUtils
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.deviceRoutes(context: Context, serverPort: Int) {
    get("/device/info") {
        call.respond(DeviceUtils.getDeviceInfo(context, serverPort))
    }
}
