package com.antigravity.remote.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.widget.Toast

/**
 * JavaScript interface injected into WebView to support easy link copying,
 * table link extraction, and link context menus on mobile devices.
 */
class WebLinkInterface(
    private val context: Context,
    private val onShowLinkActions: (url: String, text: String) -> Unit,
    private val onShowTableLinks: (title: String, jsonLinks: String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun copyToClipboard(text: String, toastMsg: String?) {
        mainHandler.post {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", text)
                clipboard.setPrimaryClip(clip)
                val msg = if (toastMsg.isNullOrBlank()) "已复制到剪贴板" else toastMsg
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "复制失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JavascriptInterface
    fun openInBrowser(url: String) {
        mainHandler.post {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开浏览器: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JavascriptInterface
    fun showLinkActions(url: String, text: String) {
        mainHandler.post {
            onShowLinkActions(url, text)
        }
    }

    @JavascriptInterface
    fun showTableLinks(title: String, jsonLinks: String) {
        mainHandler.post {
            onShowTableLinks(title, jsonLinks)
        }
    }
}
