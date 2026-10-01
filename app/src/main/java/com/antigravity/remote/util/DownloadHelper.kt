package com.antigravity.remote.util

import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.antigravity.remote.AntigravityRemoteApp
import java.io.File
import java.io.FileOutputStream

object DownloadHelper {
    private const val TAG = "DownloadHelper"

    /**
     * Download a standard HTTP/HTTPS file URL via Android system DownloadManager,
     * carrying over Google OAuth session cookies and User-Agent.
     */
    fun downloadHttpUrl(
        context: Context,
        url: String,
        userAgent: String = "",
        contentDisposition: String? = null,
        mimeType: String? = null
    ) {
        try {
            var fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val cleanUrl = url.substringBefore('?')
            val lastSegment = Uri.parse(cleanUrl).lastPathSegment
            if (!lastSegment.isNullOrBlank() && lastSegment.contains('.') && fileName.endsWith(".bin")) {
                fileName = lastSegment
            }
            fileName = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")

            val cookies = CookieManager.getInstance().getCookie(url)

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                if (!cookies.isNullOrBlank()) {
                    addRequestHeader("Cookie", cookies)
                }
                if (userAgent.isNotBlank()) {
                    addRequestHeader("User-Agent", userAgent)
                }
                setDescription("Antigravity 正在下载文件...")
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "开始下载: $fileName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager failed, falling back to external browser", e)
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (err: Exception) {
                Log.e(TAG, "External browser fallback failed", err)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "下载失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Save base64-encoded file data (from blob: or data: URIs) directly to the system Downloads folder.
     * Supports both Android 10+ (MediaStore.Downloads) and Android 8/9 (Environment.DIRECTORY_DOWNLOADS).
     */
    fun saveBase64ToDownloads(
        context: Context,
        base64Data: String,
        mimeType: String?,
        fileName: String
    ) {
        Thread {
            try {
                val cleanBase64 = if (base64Data.contains(",")) {
                    base64Data.substringAfter(",")
                } else {
                    base64Data
                }

                var effectiveMime = mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
                if (effectiveMime == "application/octet-stream" && base64Data.startsWith("data:")) {
                    val prefix = base64Data.substringBefore(";", "")
                    val detected = prefix.removePrefix("data:")
                    if (detected.isNotBlank()) effectiveMime = detected
                }

                var safeFileName = fileName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
                if (!safeFileName.contains(".")) {
                    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(effectiveMime)
                    if (!ext.isNullOrBlank()) {
                        safeFileName = "$safeFileName.$ext"
                    }
                }

                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, safeFileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, effectiveMime)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os ->
                            os.write(bytes)
                            os.flush()
                        }
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                        notifyDownloadFinished(context, safeFileName, uri, effectiveMime)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()

                    var destFile = File(downloadsDir, safeFileName)
                    var count = 1
                    val nameWithoutExt = destFile.nameWithoutExtension
                    val ext = if (destFile.extension.isNotEmpty()) ".${destFile.extension}" else ""
                    while (destFile.exists()) {
                        destFile = File(downloadsDir, "$nameWithoutExt ($count)$ext")
                        count++
                    }

                    FileOutputStream(destFile).use { fos ->
                        fos.write(bytes)
                        fos.flush()
                    }

                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destFile.absolutePath),
                        arrayOf(effectiveMime)
                    ) { _, uri ->
                        notifyDownloadFinished(context, destFile.name, uri, effectiveMime)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save base64 download", e)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "保存下载文件失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    /**
     * Notify user via Toast and system status bar notification when a file download finishes.
     */
    private fun notifyDownloadFinished(
        context: Context,
        fileName: String,
        uri: Uri?,
        mimeType: String
    ) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "下载完成: $fileName", Toast.LENGTH_LONG).show()
        }

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                if (uri != null) {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                } else {
                    setDataAndType(Uri.parse(Environment.DIRECTORY_DOWNLOADS), "*/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, AntigravityRemoteApp.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("文件下载完成")
                .setContentText(fileName)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send download notification", e)
        }
    }

    /**
     * Checks if a URL represents a downloadable asset or document.
     */
    fun isDownloadableUrl(url: String): Boolean {
        val clean = url.lowercase().substringBefore('?')
        val extensions = listOf(
            ".zip", ".tar", ".gz", ".tgz", ".bz2", ".7z", ".rar",
            ".apk", ".aab", ".jar",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".csv", ".json", ".xml", ".txt", ".md",
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg",
            ".mp4", ".mp3", ".wav",
            ".patch", ".diff", ".log"
        )
        return extensions.any { clean.endsWith(it) } ||
                url.contains("download=true") ||
                url.contains("export=true") ||
                url.contains("action=download")
    }

    /**
     * Handles WebView setDownloadListener callbacks for blob:, data:, and http/https URLs.
     */
    fun handleDownloadListener(
        context: Context,
        webView: WebView,
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimetype: String?
    ) {
        when {
            url.startsWith("blob:") -> {
                val guessedName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                val safeMime = mimetype ?: "application/octet-stream"
                val js = """
                    (function() {
                        fetch('$url')
                            .then(function(r) { return r.blob(); })
                            .then(function(b) {
                                var reader = new FileReader();
                                reader.onloadend = function() {
                                    var res = reader.result;
                                    var b64 = res.indexOf(',') !== -1 ? res.substring(res.indexOf(',') + 1) : res;
                                    if (window.AndroidBlobDownloader) {
                                        window.AndroidBlobDownloader.getBase64FromBlobData(b64, '$safeMime', '$guessedName');
                                    }
                                };
                                reader.readAsDataURL(b);
                            })
                            .catch(function(err) {
                                console.error('Blob fetch error:', err);
                            });
                    })();
                """.trimIndent()
                webView.evaluateJavascript(js, null)
            }
            url.startsWith("data:") -> {
                val guessedName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                saveBase64ToDownloads(
                    context = context,
                    base64Data = url,
                    mimeType = mimetype ?: "application/octet-stream",
                    fileName = guessedName
                )
            }
            else -> {
                downloadHttpUrl(context, url, userAgent, contentDisposition, mimetype)
            }
        }
    }
}
