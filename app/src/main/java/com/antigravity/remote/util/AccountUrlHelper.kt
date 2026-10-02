package com.antigravity.remote.util

import android.net.Uri

object AccountUrlHelper {
    private val ACCOUNT_PATH_REGEX = Regex("""/u/(\d+)(/|$)""")
    private val AUTH_USER_QUERY_REGEX = Regex("""[?&]authuser=(\d+)""")

    /**
     * Checks whether this URL is a valid application URL that should be persisted for resuming.
     * Temporary authentication redirects, login forms, and oauth consent pages are excluded.
     */
    fun isPersistableAppUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val uri = try {
            Uri.parse(url)
        } catch (_: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false

        val host = uri.host?.lowercase() ?: return false
        // Exclude Google login / OAuth authorization flows from being recorded as the startup resume URL
        if (host.contains("accounts.google.") || host.contains("accounts.youtube.")) {
            return false
        }

        val path = uri.path?.lowercase() ?: ""
        if (path.contains("/signin") || path.contains("/servicelogin") || path.contains("/oauth")) {
            return false
        }

        return true
    }

    /**
     * Extracts Google account index (e.g., 0, 1, 2) from URL (/u/0, /u/1, or authuser query param).
     */
    fun extractAccountIndex(url: String?): Int? {
        if (url.isNullOrBlank()) return null
        val pathMatch = ACCOUNT_PATH_REGEX.find(url)
        if (pathMatch != null) {
            return pathMatch.groupValues[1].toIntOrNull()
        }
        val queryMatch = AUTH_USER_QUERY_REGEX.find(url)
        if (queryMatch != null) {
            return queryMatch.groupValues[1].toIntOrNull()
        }
        return null
    }

    /**
     * Builds a URL pointing to a specific account index (/u/{accountIndex}/).
     */
    fun buildAccountUrl(baseUrlOrCurrent: String, accountIndex: Int): String {
        val raw = if (baseUrlOrCurrent.isBlank()) "https://antigravity.google.com" else baseUrlOrCurrent.trim()
        val uri = try {
            Uri.parse(raw)
        } catch (_: Exception) {
            null
        }

        // 1. If URL already has /u/\d+, replace it directly
        val uReplaceRegex = Regex("""/u/\d+""")
        if (uReplaceRegex.containsMatchIn(raw)) {
            return raw.replace(uReplaceRegex, "/u/$accountIndex")
        }

        // 2. If URL has authuser query parameter, update it
        val authUserReplaceRegex = Regex("""([?&])authuser=\d+""")
        if (authUserReplaceRegex.containsMatchIn(raw)) {
            return raw.replace(authUserReplaceRegex, "$1authuser=$accountIndex")
        }

        // 3. For antigravity.google.com domains, prepend /u/{accountIndex} to the path
        if (uri != null && uri.host?.contains("antigravity.google.com") == true) {
            val scheme = uri.scheme ?: "https"
            val host = uri.host ?: "antigravity.google.com"
            val path = uri.path ?: ""
            val query = if (uri.query.isNullOrBlank()) "" else "?${uri.query}"
            val newPath = if (path.isEmpty() || path == "/") {
                "/u/$accountIndex/"
            } else if (path.startsWith("/")) {
                "/u/$accountIndex$path"
            } else {
                "/u/$accountIndex/$path"
            }
            return "$scheme://$host$newPath$query"
        }

        // 4. Default fallback
        return if (raw.endsWith("/")) "${raw}u/$accountIndex/" else "$raw/u/$accountIndex/"
    }

    /**
     * Builds the Google Account Chooser URL which redirects back to Antigravity.
     */
    fun getAccountChooserUrl(continueUrl: String = "https://antigravity.google.com/"): String {
        val encoded = Uri.encode(continueUrl)
        return "https://accounts.google.com/AccountChooser?continue=$encoded"
    }

    /**
     * Builds the Google Add Account URL which redirects back to Antigravity.
     */
    fun getAddAccountUrl(continueUrl: String = "https://antigravity.google.com/"): String {
        val encoded = Uri.encode(continueUrl)
        return "https://accounts.google.com/AddSession?continue=$encoded"
    }
}
