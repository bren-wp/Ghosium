package com.brendigo.ghosium

/** Verified, bounded state restored after orientation changes or app restarts. */
object SessionPolicy {
    const val MAX_TABS = 8
    data class Entry(val url: String, val home: Boolean)
    data class State(val entries: List<Entry>, val selected: Int)

    fun normalize(entries: List<Entry>, selected: Int): State {
        val sanitized = entries.take(MAX_TABS).map { entry ->
            val valid = !entry.home && BrowserPolicy.isHttpsAddress(entry.url)
            if (valid) Entry(entry.url, false) else Entry("", true)
        }.ifEmpty { listOf(Entry("", true)) }
        return State(sanitized, selected.coerceIn(0, sanitized.lastIndex))
    }
}
