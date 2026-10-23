package com.brendigo.ghosium

/**
 * Routes browser input without silently opening someone else's search website.
 * URLs open the real WebView; ordinary words open native Ghosium Search.
 */
sealed class BrowserDestination {
    data class Page(val url: String) : BrowserDestination()
    data class Search(val query: String) : BrowserDestination()
    data object Invalid : BrowserDestination()

    companion object {
        fun fromInput(raw: String): BrowserDestination {
            val value = raw.trim()
            if (value.isEmpty() || value.length > 2048 || value.any { it.isISOControl() })
                return Invalid
            val lower = value.lowercase(java.util.Locale.ROOT)
            // Explicit schemes are never reinterpreted as search text, even when invalid.
            if (lower.startsWith("http://") || lower.startsWith("//") ||
                lower.matches(Regex("^[a-z][a-z0-9+.-]*:.*"))) {
                if (!lower.startsWith("https://")) return Invalid
            }
            val looksLikeUrl = lower.startsWith("https://") ||
                (!value.any { it.isWhitespace() } && !value.contains('@') && value.contains('.'))
            if (looksLikeUrl) {
                val url = UrlResolver.resolve(value) ?: return Invalid
                return Page(url)
            }
            return Search(value)
        }
    }
}
