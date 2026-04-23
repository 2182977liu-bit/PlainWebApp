package com.mobilenas.app.server

object ServerConfig {
    const val DEFAULT_PORT = 8080
    val FALLBACK_PORTS = listOf(8081, 8082, 8090, 3000)
    const val SERVER_READ_TIMEOUT_MS = 30000
}
