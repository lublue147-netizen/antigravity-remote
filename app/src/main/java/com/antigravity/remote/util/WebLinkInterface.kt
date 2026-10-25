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
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)
            val msg = if (toastMsg.isNullOrBlank()) "已复制到剪贴板" else toastMsg
            mainHandler.post {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            mainHandler.post {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Copied Text", text)
                    clipboard.setPrimaryClip(clip)
                    val msg = if (toastMsg.isNullOrBlank()) "已复制到剪贴板" else toastMsg
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } catch (ex: Exception) {
                    Toast.makeText(context, "复制失败: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @JavascriptInterface
    fun pasteFromClipboard(): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard?.hasPrimaryClip() == true) {
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val item = clipData.getItemAt(0)
                    item.coerceToText(context).toString()
                } else ""
            } else ""
        } catch (e: Exception) {
            android.util.Log.e("WebLinkInterface", "Failed to paste from clipboard", e)
            ""
        }
    }

    @JavascriptInterface
    fun hasClipboardText(): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.hasPrimaryClip() == true && (clipboard.primaryClip?.itemCount ?: 0) > 0
        } catch (_: Exception) {
            false
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
