package com.antigravity.remote.data.model

data class AppReleaseInfo(
    val tagName: String,
    val version: String,
    val title: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String,
    val apkUrl: String?,
    val apkName: String?,
    val apkSize: Long = 0L,
    val isNewer: Boolean = false,
    val isCurrent: Boolean = false
) {
    val formattedSize: String
        get() = if (apkSize > 0) {
            String.format("%.1f MB", apkSize / (1024.0 * 1024.0))
        } else {
            ""
        }
}

sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data class UpToDate(val currentVersion: String) : UpdateCheckState()
    data class NewVersionAvailable(val updateInfo: AppReleaseInfo) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}
