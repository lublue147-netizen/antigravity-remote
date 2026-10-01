package com.antigravity.remote.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * JavaScript interface injected into WebView to receive base64 encoded data from
 * blob: and data: URLs generated inside Google Antigravity web interface.
 */
class BlobDownloadInterface(private val context: Context) {

    @JavascriptInterface
    fun getBase64FromBlobData(base64Data: String, mimeType: String?, fileName: String?) {
        Handler(Looper.getMainLooper()).post {
            DownloadHelper.saveBase64ToDownloads(
                context = context,
                base64Data = base64Data,
                mimeType = mimeType,
                fileName = fileName ?: "download_${System.currentTimeMillis()}"
            )
        }
    }
}
