package com.example.plainapp

import android.app.Application
import com.example.plainapp.browser.BrowserManager
import com.example.plainapp.server.BrowserHttpHandler
import com.example.plainapp.server.SimpleWebServer

class PlainAppApplication : Application() {
    
    lateinit var browserManager: BrowserManager
        private set
    
    lateinit var browserHttpHandler: BrowserHttpHandler
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeBrowser()
    }
    
    private fun initializeBrowser() {
        browserManager = BrowserManager(this)
        browserHttpHandler = BrowserHttpHandler(this, browserManager)
    }
    
    companion object {
        lateinit var instance: PlainAppApplication
            private set
        
        fun getBrowserManager(): BrowserManager = instance.browserManager
        
        fun getBrowserHttpHandler(): BrowserHttpHandler = instance.browserHttpHandler
    }
}