package com.mobilenas.app.util

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val model: String = Build.MODEL,
    val brand: String = Build.BRAND,
    val manufacturer: String = Build.MANUFACTURER,
    val androidVersion: String = Build.VERSION.RELEASE,
    val sdkVersion: Int = Build.VERSION.SDK_INT,
    val device: String = Build.DEVICE,
    val product: String = Build.PRODUCT,
    val hardware: String = Build.HARDWARE,
    val fingerprint: String = Build.FINGERPRINT,
    val batteryLevel: Int = 0,
    val batteryCharging: Boolean = false,
    val batteryHealth: String = "unknown",
    val totalStorage: Long = 0,
    val usedStorage: Long = 0,
    val freeStorage: Long = 0,
    val wifiSSID: String = "",
    val ipAddress: String = "",
    val serverPort: Int = 0,
    val appVersion: String = "1.0.0"
)

object DeviceUtils {
    fun getDeviceInfo(context: Context, serverPort: Int): DeviceInfo {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
        val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val healthVal = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_HEALTH) ?: 0
        val health = when (healthVal) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over_voltage"
            else -> "unknown"
        }
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val total = stat.totalBytes
        val available = stat.availableBytes
        val appVersion = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0" } catch (_: Exception) { "1.0.0" }
        return DeviceInfo(
            batteryLevel = level, batteryCharging = charging, batteryHealth = health,
            totalStorage = total, usedStorage = total - available, freeStorage = available,
            wifiSSID = NetworkUtils.getWifiSSID(context), ipAddress = NetworkUtils.getLocalIpAddress(),
            serverPort = serverPort, appVersion = appVersion
        )
    }
}
