package com.brendigo.ghosium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Private, bounded session metadata. Stores URLs only, never WebView cookies or page contents. */
class TabSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("ghosium_tabs", Context.MODE_PRIVATE)

    fun save(entries: List<SessionPolicy.Entry>, selected: Int) {
        val state = SessionPolicy.normalize(entries, selected)
        val data = JSONArray()
        state.entries.forEach { item ->
            data.put(JSONObject().put("url", item.url).put("home", item.home))
        }
        prefs.edit().putString("tabs", data.toString())
            .putInt("selected", state.selected).apply()
    }

    fun restore(): SessionPolicy.State {
        return try {
            val raw = prefs.getString("tabs", null) ?: return SessionPolicy.normalize(emptyList(), 0)
            val data = JSONArray(raw)
            val entries = buildList {
                for (i in 0 until minOf(data.length(), SessionPolicy.MAX_TABS)) {
                    val item = data.optJSONObject(i) ?: continue
                    add(SessionPolicy.Entry(item.optString("url"), item.optBoolean("home", true)))
                }
            }
            SessionPolicy.normalize(entries, prefs.getInt("selected", 0))
        } catch (_: RuntimeException) {
            SessionPolicy.normalize(emptyList(), 0)
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
