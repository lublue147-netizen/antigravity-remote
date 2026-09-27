package com.antigravity.remote.data.model

data class ServerConfig(
    val serverUrl: String = "",
    val authToken: String = "",
    val useTls: Boolean = false
)