package com.mobilenas.app.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

object MDnsHelper {
    private const val TAG = "MDnsHelper"
    private const val SERVICE_TYPE = "_http._tcp"
    private const val SERVICE_NAME = "mobilenas"
    private var nsdManager: NsdManager? = null
    private var nsdServiceInfo: NsdServiceInfo? = null

    fun register(context: Context, port: Int) {
        try {
            nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            nsdServiceInfo = NsdServiceInfo().apply {
                serviceName = SERVICE_NAME
                serviceType = SERVICE_TYPE
                port = port
            }
            nsdManager?.registerService(nsdServiceInfo!!, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    Log.i(TAG, "mDNS registered: ${serviceInfo.serviceName}")
                }
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "mDNS registration failed: $errorCode")
                }
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    Log.i(TAG, "mDNS unregistered")
                }
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "mDNS unregistration failed: $errorCode")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "mDNS error", e)
        }
    }

    fun unregister() {
        try {
            nsdServiceInfo?.let { nsdManager?.unregisterService(it) }
        } catch (_: Exception) {}
        nsdServiceInfo = null
    }
}
