package com.antigravity.remote.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val WEB_REMOTE_URL = stringPreferencesKey("web_remote_url")
        val LAST_ACTIVE_URL = stringPreferencesKey("last_active_url")
        val LAST_ACCOUNT_INDEX = intPreferencesKey("last_account_index")
        val AUTO_RESTORE_ACCOUNT = booleanPreferencesKey("auto_restore_account")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val USE_TLS = booleanPreferencesKey("use_tls")
        val IMMERSIVE_MODE = booleanPreferencesKey("immersive_mode")
        val FIT_SCREEN = booleanPreferencesKey("fit_screen")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { it[Keys.SERVER_URL] ?: "" }
    val webRemoteUrl: Flow<String> = context.dataStore.data.map { it[Keys.WEB_REMOTE_URL] ?: "https://antigravity.google.com" }
    val lastActiveUrl: Flow<String> = context.dataStore.data.map { it[Keys.LAST_ACTIVE_URL] ?: "" }
    val lastAccountIndex: Flow<Int> = context.dataStore.data.map { it[Keys.LAST_ACCOUNT_INDEX] ?: 0 }
    val autoRestoreAccount: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_RESTORE_ACCOUNT] ?: true }
    val authToken: Flow<String> = context.dataStore.data.map { it[Keys.AUTH_TOKEN] ?: "" }
    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: true }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }
    val useTls: Flow<Boolean> = context.dataStore.data.map { it[Keys.USE_TLS] ?: false }
    val immersiveMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.IMMERSIVE_MODE] ?: true }
    val fitScreen: Flow<Boolean> = context.dataStore.data.map { it[Keys.FIT_SCREEN] ?: true }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[Keys.SERVER_URL] = url }
    }

    suspend fun setWebRemoteUrl(url: String) {
        context.dataStore.edit { it[Keys.WEB_REMOTE_URL] = url }
    }

    suspend fun setLastActiveUrl(url: String) {
        context.dataStore.edit { it[Keys.LAST_ACTIVE_URL] = url }
    }

    suspend fun setLastAccountIndex(index: Int) {
        context.dataStore.edit { it[Keys.LAST_ACCOUNT_INDEX] = index }
    }

    suspend fun setAutoRestoreAccount(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_RESTORE_ACCOUNT] = enabled }
    }

    suspend fun setAuthToken(token: String) {
        context.dataStore.edit { it[Keys.AUTH_TOKEN] = token }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setUseTls(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_TLS] = enabled }
    }

    suspend fun setImmersiveMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IMMERSIVE_MODE] = enabled }
    }

    suspend fun setFitScreen(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FIT_SCREEN] = enabled }
    }
}