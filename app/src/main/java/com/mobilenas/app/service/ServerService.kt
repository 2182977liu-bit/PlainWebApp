package com.mobilenas.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.mobilenas.app.R
import com.mobilenas.app.server.KtorServer
import com.mobilenas.app.server.MDnsHelper
import com.mobilenas.app.ui.ServerActivity
import com.mobilenas.app.util.NetworkUtils

class ServerService : Service() {

    companion object {
        private const val CHANNEL_ID = "mobilenas_server"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.mobilenas.app.action.START_SERVER"
        const val ACTION_STOP = "com.mobilenas.app.action.STOP_SERVER"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                KtorServer.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startServer()
            }
        }
        return START_STICKY
    }

    private fun startServer() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        KtorServer.start(applicationContext)
        // Register mDNS after a short delay to ensure server port is known
        Thread {
            try { Thread.sleep(2000) } catch (_: Exception) {}
            val url = KtorServer.getServerUrl()
            val port = url.substringAfterLast(":").toIntOrNull() ?: 8080
            MDnsHelper.register(applicationContext, port)
        }.start()
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, ServerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, ServerService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("手机NAS 运行中")
                .setContentText("地址: ${KtorServer.getServerUrl()}")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_delete, "停止", stopPendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("手机NAS 运行中")
                .setContentText("地址: ${KtorServer.getServerUrl()}")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "服务器状态",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示服务器运行状态"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        KtorServer.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
