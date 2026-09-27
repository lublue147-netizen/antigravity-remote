package com.antigravity.remote.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AgentSession(
    val id: String,
    val name: String,
    val status: SessionStatus,
    val model: String = "auto",
    val createdAt: String = "",
    val lastActivity: String = ""
)

@Serializable
enum class SessionStatus {
    RUNNING, IDLE, WAITING_FOR_INPUT, ERROR, COMPLETED
}