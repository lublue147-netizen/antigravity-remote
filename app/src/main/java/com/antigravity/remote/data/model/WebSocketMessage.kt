package com.antigravity.remote.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketMessage(
    val type: String,
    val payload: String = "",
    val sessionId: String = "",
    val data: Map<String, String> = emptyMap()
)