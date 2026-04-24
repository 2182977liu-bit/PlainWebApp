package com.mobilenas.app.service

import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mobilenas.app.server.KtorServer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class NotificationListenerService : NotificationListenerService() {

    @Serializable
    data class NotificationEvent(
        val type: String = "notification",
        val packageName: String,
        val title: String,
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        try {
            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val event = NotificationEvent(
                packageName = sbn.packageName,
                title = title,
                text = text
            )
            val json = Json.encodeToString(event)
            KtorServer.getInstance()?.eventFlow?.tryEmit(
                KtorServer.ServerEvent.NotificationEvent(json)
            )
        } catch (_: Exception) {}
    }
}
