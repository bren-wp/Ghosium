package com.brendigo.ghosium

import java.net.URI
import java.net.URLEncoder
import java.util.Locale

object UrlResolver {
    fun resolve(input: String): String? {
        val value = input.trim()
        if (value.isEmpty() || value.length > 2048 || value.any { it.isISOControl() }) return null
        val lower = value.lowercase(Locale.ROOT)
        if (lower.startsWith("http://") || lower.startsWith("//")) return null
        if (lower.startsWith("https://")) return validHttps(value)
        if (lower.contains("://") || Regex("^[a-z][a-z0-9+.-]*:", RegexOption.IGNORE_CASE).containsMatchIn(value)) return null
        if (!value.contains('@') && !value.any { it.isWhitespace() } && value.contains('.')) {
            return validHttps("https://" + value)
        }
        return "https://duckduckgo.com/?q=" + URLEncoder.encode(value, "UTF-8")
    }

    private fun validHttps(url: String): String? {
        return try {
            val uri = URI(url)
            val host = uri.host ?: return null
            if (!uri.scheme.equals("https", ignoreCase = true) ||
                uri.rawUserInfo != null || host.length > 253 ||
                !host.contains('.') || uri.port > 65535 || uri.port == 0 ||
                uri.rawAuthority?.contains('\\') == true) return null
            url
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
