package com.mobilenas.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mobilenas.app.server.SimpleWebServer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.mobilenas.app.R

class BrowserActivity : AppCompatActivity() {
    
    private var serverUrlText: TextView? = null
    private var startServerBtn: Button? = null
    private var stopServerBtn: Button? = null
    private var openBrowserBtn: Button? = null
    
    private var isServerRunning = false
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startServer()
        } else {
            Toast.makeText(this, "需要权限才能启动服务器", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)
        
        initViews()
        setupListeners()
        updateServerStatus()
    }
    
    private fun initViews() {
        serverUrlText = findViewById(R.id.serverUrlText)
        startServerBtn = findViewById(R.id.startServerBtn)
        stopServerBtn = findViewById(R.id.stopServerBtn)
        openBrowserBtn = findViewById(R.id.openBrowserBtn)
    }
    
    private fun setupListeners() {
        startServerBtn?.setOnClickListener {
            checkPermissionsAndStartServer()
        }
        
        stopServerBtn?.setOnClickListener {
            stopServer()
        }
        
        openBrowserBtn?.setOnClickListener {
            openBrowser()
        }
    }
    
    private fun checkPermissionsAndStartServer() {
        val permissions = mutableListOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (notGranted.isEmpty()) {
            startServer()
        } else {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        }
    }
    
    private fun startServer() {
        val server = SimpleWebServer.start(this)
        if (server != null) {
            isServerRunning = true
            updateServerStatus()
            Toast.makeText(this, "服务器已启动", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "服务器启动失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopServer() {
        SimpleWebServer.stop()
        isServerRunning = false
        updateServerStatus()
        Toast.makeText(this, "服务器已停止", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateServerStatus() {
        if (isServerRunning) {
            val url = SimpleWebServer.getServerUrl()
            serverUrlText?.text = "服务器地址: $url/browser.html"
            serverUrlText?.visibility = TextView.VISIBLE
            startServerBtn?.isEnabled = false
            stopServerBtn?.isEnabled = true
            openBrowserBtn?.isEnabled = true
        } else {
            serverUrlText?.text = "服务器未运行"
            serverUrlText?.visibility = TextView.GONE
            startServerBtn?.isEnabled = true
            stopServerBtn?.isEnabled = false
            openBrowserBtn?.isEnabled = false
        }
    }
    
    private fun openBrowser() {
        val url = SimpleWebServer.getServerUrl() + "/browser.html"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isServerRunning) {
            SimpleWebServer.stop()
            isServerRunning = false
        }
        serverUrlText = null
        startServerBtn = null
        stopServerBtn = null
        openBrowserBtn = null
    }
}