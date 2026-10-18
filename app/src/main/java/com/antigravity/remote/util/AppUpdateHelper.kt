package com.antigravity.remote.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.antigravity.remote.BuildConfig
import com.antigravity.remote.data.model.AppReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AppUpdateHelper {
    private const val TAG = "AppUpdateHelper"
    private const val RELEASES_API_URL = "https://api.github.com/repos/lublue147-netizen/antigravity-remote/releases"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val currentVersionName: String
        get() = BuildConfig.VERSION_NAME

    /**
     * Compare two semantic versions (e.g. "v1.0.12" vs "1.0.11").
     * Returns > 0 if versionA > versionB, < 0 if versionA < versionB, 0 if equal.
     */
    fun compareVersions(versionA: String, versionB: String): Int {
        val cleanA = versionA.trim().removePrefix("v").removePrefix("V")
        val cleanB = versionB.trim().removePrefix("v").removePrefix("V")

        val regex = Regex("""\d+""")
        val partsA = regex.findAll(cleanA).map { it.value.toIntOrNull() ?: 0 }.toList()
        val partsB = regex.findAll(cleanB).map { it.value.toIntOrNull() ?: 0 }.toList()

        val maxLength = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLength) {
            val numA = partsA.getOrElse(i) { 0 }
            val numB = partsB.getOrElse(i) { 0 }
            if (numA != numB) {
                return numA.compareTo(numB)
            }
        }
        return 0
    }

    /**
     * Fetches all available releases from GitHub API.
     */
    suspend fun fetchAvailableReleases(): Result<List<AppReleaseInfo>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RELEASES_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AntigravityRemoteAndroid/$currentVersionName")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub API 响应失败: HTTP ${response.code}"))
            }

            val bodyString = response.body?.string() ?: ""
            if (bodyString.isBlank()) {
                return@withContext Result.failure(Exception("未获取到版本数据"))
            }

            val array = JSONArray(bodyString)
            val list = mutableListOf<AppReleaseInfo>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optBoolean("draft", false)) continue

                val info = parseReleaseObject(obj)
                list.add(info)
            }

            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch releases", e)
            Result.failure(e)
        }
    }

    /**
     * Checks for the latest release and evaluates whether a newer version exists.
     */
    suspend fun checkLatestRelease(): Result<AppReleaseInfo?> = withContext(Dispatchers.IO) {
        val releasesResult = fetchAvailableReleases()
        if (releasesResult.isFailure) {
            return@withContext Result.failure(releasesResult.exceptionOrNull() ?: Exception("检查更新失败"))
        }

        val list = releasesResult.getOrNull() ?: emptyList()
        val latest = list.firstOrNull()
        Result.success(latest)
    }

    private fun parseReleaseObject(obj: JSONObject): AppReleaseInfo {
        val tagName = obj.optString("tag_name", "").trim()
        val version = tagName.removePrefix("v").removePrefix("V")
        val title = obj.optString("name", tagName).ifBlank { tagName }
        val body = obj.optString("body", "").trim()
        val publishedAt = obj.optString("published_at", "").trim()
        val htmlUrl = obj.optString("html_url", "")

        var apkUrl: String? = null
        var apkName: String? = null
        var apkSize: Long = 0L

        val assetsArray = obj.optJSONArray("assets")
        if (assetsArray != null) {
            for (j in 0 until assetsArray.length()) {
                val asset = assetsArray.getJSONObject(j)
                val aName = asset.optString("name", "")
                val dlUrl = asset.optString("browser_download_url", "")
                val size = asset.optLong("size", 0L)

                if (aName.endsWith("-release.apk", ignoreCase = true)) {
                    apkUrl = dlUrl
                    apkName = aName
                    apkSize = size
                    break
                } else if (aName.endsWith(".apk", ignoreCase = true) && apkUrl == null) {
                    apkUrl = dlUrl
                    apkName = aName
                    apkSize = size
                }
            }
        }

        val comparison = compareVersions(version, currentVersionName)
        val isNewer = comparison > 0
        val isCurrent = comparison == 0

        return AppReleaseInfo(
            tagName = tagName,
            version = version,
            title = title,
            body = body,
            publishedAt = publishedAt,
            htmlUrl = htmlUrl,
            apkUrl = apkUrl,
            apkName = apkName,
            apkSize = apkSize,
            isNewer = isNewer,
            isCurrent = isCurrent
        )
    }

    /**
     * Downloads the APK file via system DownloadManager and triggers the package installer.
     */
    fun downloadAndInstallApk(context: Context, release: AppReleaseInfo) {
        val url = release.apkUrl
        if (url.isNullOrBlank()) {
            Toast.makeText(context, "未找到有效的 APK 安装包下载链接", Toast.LENGTH_SHORT).show()
            if (release.htmlUrl.isNotBlank()) {
                openInBrowser(context, release.htmlUrl)
            }
            return
        }

        // Check install unknown packages permission on Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(context, "请在设置中开启【允许安装未知应用】以完成更新", Toast.LENGTH_LONG).show()
                try {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(manageIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Cannot launch unknown app sources setting", e)
                }
            }
        }

        val fileName = release.apkName ?: "AntigravityRemote-${release.tagName}.apk"

        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("下载 Antigravity Remote ${release.tagName}")
                setDescription("正在下载新版本安装包...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            Toast.makeText(context, "开始下载新版本: $fileName", Toast.LENGTH_SHORT).show()

            // Listen for download complete broadcast to automatically prompt installation
            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            val uri = downloadManager.getUriForDownloadedFile(downloadId)
                            if (uri != null && c != null) {
                                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/vnd.android.package-archive")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                c.startActivity(installIntent)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Auto install failed", e)
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(c, "下载已完成，请在通知栏或下载目录中点击安装", Toast.LENGTH_LONG).show()
                            }
                        } finally {
                            try {
                                context.unregisterReceiver(this)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            ContextCompat.registerReceiver(
                context,
                onCompleteReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager failed to enqueue APK", e)
            Toast.makeText(context, "下载器启动失败，正在为您打开浏览器下载", Toast.LENGTH_SHORT).show()
            openInBrowser(context, url)
        }
    }

    /**
     * Opens an external URL in the default browser.
     */
    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open browser", e)
            Toast.makeText(context, "无法唤起浏览器: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
