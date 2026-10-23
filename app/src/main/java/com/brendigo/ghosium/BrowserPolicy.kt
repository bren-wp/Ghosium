package com.brendigo.ghosium

/** Shared pure navigation/data rules, independently testable on the JVM. */
object BrowserPolicy {
    fun isHttpsAddress(url: String?): Boolean =
        url != null && UrlResolver.resolve(url) == url && url.startsWith("https://")

    fun shouldRecordVisit(url: String?, isHome: Boolean, failed: Boolean): Boolean =
        !isHome && !failed && isHttpsAddress(url)

    /** Remove unsafe filename characters before passing to Android DownloadManager. */
    fun safeFileName(original: String): String {
        val cleaned = original.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[\\p{Cntrl}<>:\"/\\\\|?*]"), "_")
            .trim().trim('.').take(120)
        return cleaned.ifBlank { "ghosium-download" }
    }
}
