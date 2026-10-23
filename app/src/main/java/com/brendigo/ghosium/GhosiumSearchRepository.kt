package com.brendigo.ghosium

import android.text.Html
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

data class GhosiumResult(val title: String, val url: String, val description: String, val source: String)

/**
 * Results are rendered by Ghosium's own native UI.
 * Optional general web source: a Brendigo-controlled SearXNG HTTPS instance.
 * Without it, the explicitly attributed Wikimedia fallback offers limited encyclopedia results,
 * NOT a general web search. No credential or API secret is embedded in an APK.
 */
class GhosiumSearchRepository {
    companion object {
        // Change only when a genuinely Brendigo-operated HTTPS SearXNG endpoint is deployed.
        // Do not use random public instances or put upstream API keys into the Android app.
        const val WEB_SEARCH_BASE_URL = ""
    }
    private val executor = Executors.newFixedThreadPool(2)

    fun search(query: String, callback: (Result<List<GhosiumResult>>) -> Unit) {
        val q = query.trim().take(200)
        if (q.isEmpty()) {
            callback(Result.success(emptyList()))
            return
        }
        executor.execute {
            try {
                val base = WEB_SEARCH_BASE_URL.trimEnd('/')
                val data = if (base.isNotEmpty() && base.startsWith("https://")) {
                    val endpoint = base + "/search?format=json&language=hr&q=" +
                        URLEncoder.encode(q, "UTF-8")
                    parseSearx(read(endpoint))
                } else {
                    val endpoint = "https://hr.wikipedia.org/w/api.php?action=query&list=search" +
                        "&format=json&srlimit=12&srsearch=" + URLEncoder.encode(q, "UTF-8")
                    parseWiki(read(endpoint))
                }
                callback(Result.success(data))
            } catch (error: Exception) {
                callback(Result.failure(error))
            }
        }
    }

    private fun read(endpoint: String): String {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection)
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent",
                "GhosiumBrowser/0.1.4 (Brendigo; https://brendigo.com)")
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode != 200) throw java.io.IOException("HTTP " + conn.responseCode)
            if (conn.contentLengthLong > 750_000) throw java.io.IOException("Odgovor je prevelik")
            conn.inputStream.bufferedReader(Charsets.UTF_8).use {
                val chars = CharArray(4096)
                val result = StringBuilder()
                while (true) {
                    val count = it.read(chars)
                    if (count < 0) break
                    result.append(chars, 0, count)
                    if (result.length > 750_000) throw java.io.IOException("Odgovor je prevelik")
                }
                return result.toString()
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun plain(html: String): String = Html.fromHtml(
        html.take(2000), Html.FROM_HTML_MODE_COMPACT).toString().trim().take(260)

    private fun parseWiki(raw: String): List<GhosiumResult> {
        val array = JSONObject(raw).optJSONObject("query")?.optJSONArray("search")
            ?: return emptyList()
        return buildList {
            for (i in 0 until minOf(12, array.length())) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optLong("pageid", -1)
                if (id <= 0) continue
                add(GhosiumResult(
                    item.optString("title").take(140),
                    "https://hr.wikipedia.org/?curid=" + id,
                    plain(item.optString("snippet")),
                    "Wikipedia · ograničena pretraga"
                ))
            }
        }
    }

    private fun parseSearx(raw: String): List<GhosiumResult> {
        val array = JSONObject(raw).optJSONArray("results") ?: return emptyList()
        return buildList {
            for (i in 0 until minOf(30, array.length())) {
                if (size >= 12) break
                val item = array.optJSONObject(i) ?: continue
                val url = item.optString("url")
                if (!BrowserPolicy.isHttpsAddress(url)) continue
                add(GhosiumResult(
                    item.optString("title").take(140).ifBlank { url },
                    url,
                    plain(item.optString("content")),
                    item.optString("engine").ifBlank { "Web" }.take(40)
                ))
            }
        }
    }
}