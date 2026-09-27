package com.antigravity.remote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.remote.data.local.SettingsDataStore
import com.antigravity.remote.data.model.*
import com.antigravity.remote.data.repository.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AgentRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val connectionState = repository.connectionState
    val sessions = repository.sessions
    val messages = repository.messages

    val serverUrl = settingsDataStore.serverUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val webRemoteUrl = settingsDataStore.webRemoteUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "https://antigravity.google.com"
    )
    val authToken = settingsDataStore.authToken.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val darkTheme = settingsDataStore.darkTheme.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    val notificationsEnabled = settingsDataStore.notificationsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    private val _selectedSessionId = MutableStateFlow<String?>(null)
    val selectedSessionId = _selectedSessionId.asStateFlow()

    fun connect() {
        viewModelScope.launch {
            val url = serverUrl.value
            val token = authToken.value
            if (url.isNotBlank()) {
                repository.connect(url, token)
            }
        }
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun selectSession(sessionId: String?) {
        _selectedSessionId.value = sessionId
    }

    fun sendMessage(content: String) {
        val sessionId = _selectedSessionId.value ?: return
        repository.sendMessage(sessionId, content)
    }

    fun approveChange() {
        val sessionId = _selectedSessionId.value ?: return
        repository.approveChange(sessionId)
    }

    fun rejectChange() {
        val sessionId = _selectedSessionId.value ?: return
        repository.rejectChange(sessionId)
    }

    fun startNewSession(name: String) {
        repository.startNewSession(name)
    }

    fun stopSession(sessionId: String) {
        repository.stopSession(sessionId)
    }

    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.setServerUrl(url)
            if (url.contains("antigravity.google.com")) {
                settingsDataStore.setWebRemoteUrl(url)
            }
        }
    }

    fun updateWebRemoteUrl(url: String) {
        viewModelScope.launch { settingsDataStore.setWebRemoteUrl(url) }
    }

    fun updateAuthToken(token: String) {
        viewModelScope.launch { settingsDataStore.setAuthToken(token) }
    }

    fun updateDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDarkTheme(enabled) }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setNotificationsEnabled(enabled) }
    }
}