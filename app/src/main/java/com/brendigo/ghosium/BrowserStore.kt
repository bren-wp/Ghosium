package com.brendigo.ghosium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BrowserStore(context: Context) {
    data class Item(val title: String, val url: String)
    private val prefs = context.getSharedPreferences("ghosium_data", Context.MODE_PRIVATE)
    val bookmarks = load("bookmarks")
    val history = load("history")
    private fun load(key: String): MutableList<Item> {
        val out = mutableListOf<Item>()
        try {
            val array = JSONArray(prefs.getString(key, "[]"))
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val url = item.optString("url")
                if (url.startsWith("https://")) out.add(Item(item.optString("title").take(150), url.take(2048)))
            }
        } catch (_: Exception) { }
        return out
    }
    private fun save(key: String, items: List<Item>) {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("title", it.title).put("url", it.url)) }
        prefs.edit().putString(key, array.toString()).apply()
    }
    fun toggleBookmark(title: String, url: String): Boolean {
        if (!url.startsWith("https://")) return false
        val old = bookmarks.indexOfFirst { it.url == url }
        if (old >= 0) { bookmarks.removeAt(old); save("bookmarks", bookmarks); return false }
        bookmarks.add(0, Item(title.take(150), url.take(2048)))
        while (bookmarks.size > 150) bookmarks.removeAt(bookmarks.lastIndex)
        save("bookmarks", bookmarks)
        return true
    }
    fun addHistory(title: String, url: String) {
        if (!url.startsWith("https://")) return
        history.removeAll { it.url == url }
        history.add(0, Item(title.take(150), url.take(2048)))
        while (history.size > 200) history.removeAt(history.lastIndex)
        save("history", history)
    }
    fun removeHistory(url: String) {
        history.removeAll { it.url == url }
        save("history", history)
    }
    fun clearHistory() { history.clear(); prefs.edit().remove("history").apply() }
}
