package com.mobilenas.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mobilenas.app.R
import com.mobilenas.app.server.KtorServer
import com.mobilenas.app.service.ServerService
import com.mobilenas.app.util.NetworkUtils

class ServerActivity : AppCompatActivity() {

    private lateinit var serverUrlText: TextView
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var openBrowserBtn: Button

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startServer()
        } else {
            Toast.makeText(this, "需要存储权限才能管理文件", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        serverUrlText = findViewById(R.id.serverUrlText)
        startBtn = findViewById(R.id.startServerBtn)
        stopBtn = findViewById(R.id.stopServerBtn)
        openBrowserBtn = findViewById(R.id.openBrowserBtn)

        startBtn.setOnClickListener { checkPermissionsAndStart() }
        stopBtn.setOnClickListener { stopServer() }
        openBrowserBtn.setOnClickListener { openBrowser() }

        updateUI()
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            startServer()
        }
    }

    private fun startServer() {
        val intent = Intent(this, ServerService::class.java).apply {
            action = ServerService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        // Wait a moment for server to start, then update UI
        serverUrlText.postDelayed({ updateUI() }, 1500)
    }

    private fun stopServer() {
        val intent = Intent(this, ServerService::class.java).apply {
            action = ServerService.ACTION_STOP
        }
        startService(intent)
        serverUrlText.postDelayed({ updateUI() }, 500)
    }

    private fun openBrowser() {
        val url = KtorServer.getServerUrl()
        if (url.isEmpty() || !KtorServer.isRunning()) {
            Toast.makeText(this, "服务器未启动", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
        startActivity(intent)
    }

    private fun updateUI() {
        val running = KtorServer.isRunning()
        startBtn.isEnabled = !running
        stopBtn.isEnabled = running
        openBrowserBtn.isEnabled = running
        if (running) {
            serverUrlText.text = KtorServer.getServerUrl()
        } else {
            serverUrlText.text = "服务器未启动"
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }
}
