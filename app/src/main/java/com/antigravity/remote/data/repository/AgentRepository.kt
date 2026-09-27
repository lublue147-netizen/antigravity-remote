package com.antigravity.remote.data.repository

import com.antigravity.remote.data.local.SettingsDataStore
import com.antigravity.remote.data.model.*
import com.antigravity.remote.data.remote.WebSocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRepository @Inject constructor(
    private val webSocketService: WebSocketService,
    private val settingsDataStore: SettingsDataStore
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _sessions = MutableStateFlow<List<AgentSession>>(emptyList())
    val sessions: StateFlow<List<AgentSession>> = _sessions.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    val connectionState = webSocketService.connectionState

    init {
        scope.launch {
            for (msg in webSocketService.messageChannel) {
                handleMessage(msg)
            }
        }
    }

    fun connect(serverUrl: String, authToken: String) {
        if (serverUrl.contains("antigravity.google.com")) {
            webSocketService.setError("antigravity.google.com 为官方网页版。请在底部选择【网页版】直接访问，或在设置中输入自建 WebSocket 服务地址。")
            return
        }
        val wsUrl = buildWsUrl(serverUrl)
        webSocketService.connect(wsUrl, authToken)
    }

    fun disconnect() {
        webSocketService.disconnect()
        _sessions.value = emptyList()
        _messages.value = emptyMap()
    }

    fun sendMessage(sessionId: String, content: String) {
        val message = WebSocketMessage(
            type = "user_message",
            sessionId = sessionId,
            payload = content
        )
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = MessageRole.USER,
            content = content
        )
        addMessage(sessionId, userMsg)
        webSocketService.send(json.encodeToString(WebSocketMessage.serializer(), message))
    }

    fun approveChange(sessionId: String) {
        val message = WebSocketMessage(
            type = "approve",
            sessionId = sessionId
        )
        webSocketService.send(json.encodeToString(WebSocketMessage.serializer(), message))
    }

    fun rejectChange(sessionId: String) {
        val message = WebSocketMessage(
            type = "reject",
            sessionId = sessionId
        )
        webSocketService.send(json.encodeToString(WebSocketMessage.serializer(), message))
    }

    fun startNewSession(name: String) {
        val message = WebSocketMessage(
            type = "new_session",
            payload = name
        )
        webSocketService.send(json.encodeToString(WebSocketMessage.serializer(), message))
    }

    fun stopSession(sessionId: String) {
        val message = WebSocketMessage(
            type = "stop_session",
            sessionId = sessionId
        )
        webSocketService.send(json.encodeToString(WebSocketMessage.serializer(), message))
    }

    private fun handleMessage(rawMsg: String) {
        try {
            val wsMsg = json.decodeFromString(WebSocketMessage.serializer(), rawMsg)
            when (wsMsg.type) {
                "sessions_list" -> {
                    val sessionsList = json.decodeFromString<List<AgentSession>>(wsMsg.payload)
                    _sessions.value = sessionsList
                }
                "session_update" -> {
                    val session = json.decodeFromString<AgentSession>(wsMsg.payload)
                    _sessions.value = _sessions.value.map {
                        if (it.id == session.id) session else it
                    }
                }
                "assistant_message", "system_message", "tool_message" -> {
                    val role = when (wsMsg.type) {
                        "assistant_message" -> MessageRole.ASSISTANT
                        "system_message" -> MessageRole.SYSTEM
                        else -> MessageRole.TOOL
                    }
                    val chatMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sessionId = wsMsg.sessionId,
                        role = role,
                        content = wsMsg.payload
                    )
                    addMessage(wsMsg.sessionId, chatMsg)
                }
                "stream_chunk" -> {
                    val currentMessages = _messages.value[wsMsg.sessionId]?.toMutableList() ?: mutableListOf()
                    val lastMsg = currentMessages.lastOrNull()
                    if (lastMsg != null && lastMsg.role == MessageRole.ASSISTANT && lastMsg.isStreaming) {
                        currentMessages[currentMessages.lastIndex] = lastMsg.copy(
                            content = lastMsg.content + wsMsg.payload
                        )
                    } else {
                        currentMessages.add(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                sessionId = wsMsg.sessionId,
                                role = MessageRole.ASSISTANT,
                                content = wsMsg.payload,
                                isStreaming = true
                            )
                        )
                    }
                    _messages.value = _messages.value.toMutableMap().apply {
                        put(wsMsg.sessionId, currentMessages)
                    }
                }
                "stream_end" -> {
                    val currentMessages = _messages.value[wsMsg.sessionId]?.toMutableList() ?: return
                    val lastMsg = currentMessages.lastOrNull() ?: return
                    if (lastMsg.isStreaming) {
                        currentMessages[currentMessages.lastIndex] = lastMsg.copy(isStreaming = false)
                        _messages.value = _messages.value.toMutableMap().apply {
                            put(wsMsg.sessionId, currentMessages)
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun addMessage(sessionId: String, message: ChatMessage) {
        val currentMessages = _messages.value[sessionId]?.toMutableList() ?: mutableListOf()
        currentMessages.add(message)
        _messages.value = _messages.value.toMutableMap().apply {
            put(sessionId, currentMessages)
        }
    }

    private fun buildWsUrl(serverUrl: String): String {
        val cleanUrl = serverUrl.trimEnd('/')
        return when {
            cleanUrl.startsWith("ws://") || cleanUrl.startsWith("wss://") -> "$cleanUrl/ws"
            cleanUrl.startsWith("https://") -> "wss://${cleanUrl.removePrefix("https://")}/ws"
            cleanUrl.startsWith("http://") -> "ws://${cleanUrl.removePrefix("http://")}/ws"
            else -> "ws://$cleanUrl/ws"
        }
    }
}