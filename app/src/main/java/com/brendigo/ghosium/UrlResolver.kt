package com.brendigo.ghosium

import java.net.URLEncoder

object UrlResolver {
    fun resolve(input: String): String? {
        val value = input.trim()
        if (value.isEmpty()) return null
        val lower = value.lowercase()
        if (lower.startsWith("http://")) return null
        if (lower.startsWith("https://")) {
            val rest = value.substring(8)
            if (rest.isBlank() || rest.any { it.isWhitespace() } || !rest.substringBefore('/').contains('.')) return null
            return value
        }
        if (lower.contains("://") || lower.startsWith("file:") || lower.startsWith("javascript:") || lower.startsWith("content:")) return null
        if (value.contains('.') && !value.contains(' ') && !value.contains('@')) return "https://" + value
        return "https://duckduckgo.com/?q=" + URLEncoder.encode(value, "UTF-8")
    }
}
