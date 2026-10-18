package com.antigravity.remote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.remote.data.local.SettingsDataStore
import com.antigravity.remote.data.model.*
import com.antigravity.remote.data.repository.AgentRepository
import com.antigravity.remote.util.AccountUrlHelper
import com.antigravity.remote.util.AppUpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AgentRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val isSettingsLoaded = settingsDataStore.isLoaded.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    val connectionState = repository.connectionState
    val sessions = repository.sessions
    val messages = repository.messages

    val serverUrl = settingsDataStore.serverUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val webRemoteUrl = settingsDataStore.webRemoteUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "https://antigravity.google.com"
    )
    val lastActiveUrl = settingsDataStore.lastActiveUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val lastAccountIndex = settingsDataStore.lastAccountIndex.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val autoRestoreAccount = settingsDataStore.autoRestoreAccount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
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
    val immersiveMode = settingsDataStore.immersiveMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    val fitScreen = settingsDataStore.fitScreen.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    val autoCheckUpdates = settingsDataStore.autoCheckUpdates.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    private val _availableReleases = MutableStateFlow<List<AppReleaseInfo>>(emptyList())
    val availableReleases: StateFlow<List<AppReleaseInfo>> = _availableReleases.asStateFlow()

    private val _isLoadingReleases = MutableStateFlow(false)
    val isLoadingReleases: StateFlow<Boolean> = _isLoadingReleases.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _selectedReleaseForDialog = MutableStateFlow<AppReleaseInfo?>(null)
    val selectedReleaseForDialog: StateFlow<AppReleaseInfo?> = _selectedReleaseForDialog.asStateFlow()

    private val _showVersionPickerDialog = MutableStateFlow(false)
    val showVersionPickerDialog: StateFlow<Boolean> = _showVersionPickerDialog.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.autoCheckUpdates.first().let { enabled ->
                if (enabled) {
                    kotlinx.coroutines.delay(2500)
                    checkForUpdates(isManual = false)
                }
            }
        }
    }

    private val _pendingLoadUrl = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val pendingLoadUrl = _pendingLoadUrl.asSharedFlow()

    fun requestLoadUrl(url: String) {
        _pendingLoadUrl.tryEmit(url)
    }

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

    fun recordActiveUrl(url: String) {
        if (!AccountUrlHelper.isPersistableAppUrl(url)) return
        val cleanUrl = url.trim()
        if (cleanUrl == lastActiveUrl.value) return
        viewModelScope.launch {
            settingsDataStore.setLastActiveUrl(cleanUrl)
            val detectedIndex = AccountUrlHelper.extractAccountIndex(cleanUrl)
            if (detectedIndex != null && detectedIndex != lastAccountIndex.value) {
                settingsDataStore.setLastAccountIndex(detectedIndex)
            }
            if (cleanUrl != webRemoteUrl.value) {
                // Keep webRemoteUrl synchronized with active account URL
                settingsDataStore.setWebRemoteUrl(cleanUrl)
            }
        }
    }

    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.setServerUrl(url)
            if (url.contains("antigravity.google.com")) {
                settingsDataStore.setWebRemoteUrl(url)
                if (AccountUrlHelper.isPersistableAppUrl(url)) {
                    settingsDataStore.setLastActiveUrl(url)
                    val idx = AccountUrlHelper.extractAccountIndex(url)
                    if (idx != null) {
                        settingsDataStore.setLastAccountIndex(idx)
                    }
                }
            }
        }
    }

    fun updateWebRemoteUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.setWebRemoteUrl(url)
            if (AccountUrlHelper.isPersistableAppUrl(url)) {
                settingsDataStore.setLastActiveUrl(url)
                val idx = AccountUrlHelper.extractAccountIndex(url)
                if (idx != null) {
                    settingsDataStore.setLastAccountIndex(idx)
                }
            }
        }
    }

    fun updateLastActiveUrl(url: String) {
        viewModelScope.launch { settingsDataStore.setLastActiveUrl(url) }
    }

    fun updateLastAccountIndex(index: Int) {
        viewModelScope.launch { settingsDataStore.setLastAccountIndex(index) }
    }

    fun updateAutoRestoreAccount(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAutoRestoreAccount(enabled) }
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

    fun updateImmersiveMode(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setImmersiveMode(enabled) }
    }

    fun updateFitScreen(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setFitScreen(enabled) }
    }

    fun updateAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAutoCheckUpdates(enabled) }
    }

    fun checkForUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckState.Checking
            val result = AppUpdateHelper.checkLatestRelease()
            result.fold(
                onSuccess = { latest ->
                    if (latest != null && latest.isNewer) {
                        _updateCheckState.value = UpdateCheckState.NewVersionAvailable(latest)
                        _selectedReleaseForDialog.value = latest
                        _showUpdateDialog.value = true
                    } else {
                        _updateCheckState.value = UpdateCheckState.UpToDate(AppUpdateHelper.currentVersionName)
                        if (isManual) {
                            if (latest != null) {
                                _selectedReleaseForDialog.value = latest
                                _showUpdateDialog.value = true
                            }
                        }
                    }
                },
                onFailure = { err ->
                    _updateCheckState.value = UpdateCheckState.Error(err.localizedMessage ?: "检查更新失败")
                }
            )
        }
    }

    fun fetchAllReleases() {
        viewModelScope.launch {
            _isLoadingReleases.value = true
            _showVersionPickerDialog.value = true
            val result = AppUpdateHelper.fetchAvailableReleases()
            result.onSuccess { list ->
                _availableReleases.value = list
            }
            _isLoadingReleases.value = false
        }
    }

    fun selectReleaseForUpdate(release: AppReleaseInfo) {
        _selectedReleaseForDialog.value = release
        _showVersionPickerDialog.value = false
        _showUpdateDialog.value = true
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    fun dismissVersionPickerDialog() {
        _showVersionPickerDialog.value = false
    }
}