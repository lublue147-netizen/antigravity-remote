package com.antigravity.remote.data.remote

import com.antigravity.remote.data.model.ConnectionState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketService @Inject constructor() {

    private var webSocket: WebSocket? = null
    private var lastUrl: String = ""
    private var lastToken: String = ""
    private var isUserDisconnected: Boolean = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val messageChannel = Channel<String>(Channel.BUFFERED)

    private var reconnectAttempt = 0

    fun connect(url: String, token: String = "") {
        reconnectJob?.cancel()
        isUserDisconnected = false
        lastUrl = url
        lastToken = token
        _connectionState.value = ConnectionState.Connecting

        val requestBuilder = Request.Builder().url(url)
        if (token.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        webSocket = client.newWebSocket(requestBuilder.build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempt = 0
                _connectionState.value = ConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                messageChannel.trySend(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                _connectionState.value = ConnectionState.Disconnected
                if (!isUserDisconnected) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.Error(t.message ?: "Connection failed")
                if (!isUserDisconnected) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        if (isUserDisconnected || lastUrl.isBlank()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val backoffMs = (3000L * (1L shl reconnectAttempt.coerceAtMost(3))).coerceAtMost(30000L)
            reconnectAttempt++
            delay(backoffMs)
            if (!isUserDisconnected && lastUrl.isNotBlank()) {
                connect(lastUrl, lastToken)
            }
        }
    }

    fun send(message: String): Boolean {
        return webSocket?.send(message) ?: false
    }

    fun disconnect() {
        isUserDisconnected = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun setError(message: String) {
        _connectionState.value = ConnectionState.Error(message)
    }
}