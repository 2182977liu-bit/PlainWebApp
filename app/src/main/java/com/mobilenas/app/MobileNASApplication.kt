package com.mobilenas.app

import android.app.Application
import com.mobilenas.app.browser.BrowserManager
import com.mobilenas.app.server.BrowserHttpHandler
import com.mobilenas.app.server.SimpleWebServer

class MobileNASApplication : Application() {

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
        lateinit var instance: MobileNASApplication
            private set

        fun getBrowserManager(): BrowserManager = instance.browserManager

        fun getBrowserHttpHandler(): BrowserHttpHandler = instance.browserHttpHandler
    }
}
