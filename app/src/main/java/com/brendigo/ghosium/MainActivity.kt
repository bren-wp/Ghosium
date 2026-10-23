package com.brendigo.ghosium

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

/**
 * Ghosium Android MVP.
 * Renders websites using the installed Android System WebView.
 * This implementation DOES NOT claim isolated incognito or ad blocking.
 */
class MainActivity : Activity() {
    private companion object {
        const val BG = 0xFF090D16.toInt()
        const val PANEL = 0xFF141C2A.toInt()
        const val FG = 0xFFF6F8FC.toInt()
        const val DIM = 0xFF9BAAC0.toInt()
        const val ACCENT = 0xFF58F4CD.toInt()
        const val MAX_TABS = 8
    }

    private data class Tab(
        val web: WebView,
        var title: String = "Nova kartica",
        var url: String = "",
        var home: Boolean = true
    )

    private lateinit var store: BrowserStore
    private lateinit var address: EditText
    private lateinit var content: FrameLayout
    private lateinit var progress: ProgressBar
    private lateinit var tabButton: TextView
    private lateinit var starButton: TextView
    private lateinit var menuButton: TextView
    private val tabs = mutableListOf<Tab>()
    private val settingsPrefs by lazy { getSharedPreferences("ghosium_settings", Context.MODE_PRIVATE) }
    private var selected = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = BrowserStore(this)
        CookieManager.getInstance().setAcceptCookie(settingsPrefs.getBoolean("cookies", true))
        buildUi()
        createTab()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun shape(color: Int = PANEL, radius: Int = 18): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun text(value: String, size: Float, color: Int = FG, heavy: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (heavy) setTypeface(typeface, Typeface.BOLD)
        }

    private fun action(value: String, description: String, onClick: () -> Unit): TextView =
        text(value, 23f).apply {
            gravity = Gravity.CENTER
            background = shape()
            contentDescription = description
            setOnClickListener { onClick() }
        }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setOnApplyWindowInsetsListener { v, insets ->
                @Suppress("DEPRECATION")
                v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
                insets
            }
        }
        setContentView(root)
        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(8))
        }
        top.addView(action("⌂", "Početna") { showHome() }, LinearLayout.LayoutParams(dp(48), dp(50)))
        address = EditText(this).apply {
            setSingleLine(true)
            textSize = 15f
            setTextColor(FG)
            setHintTextColor(DIM)
            hint = "Ghosium Search ili URL"
            background = shape()
            setPadding(dp(15), 0, dp(15), 0)
            setSelectAllOnFocus(true)
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) { navigate(address.text.toString()); true } else false
            }
        }
        top.addView(address, LinearLayout.LayoutParams(0, dp(50), 1f).apply {
            marginStart = dp(8)
            marginEnd = dp(8)
        })
        top.addView(action("↗", "Otvori adresu") { navigate(address.text.toString()) },
            LinearLayout.LayoutParams(dp(48), dp(50)))
        root.addView(top)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = ColorStateList.valueOf(ACCENT)
            progressBackgroundTintList = ColorStateList.valueOf(BG)
            visibility = View.INVISIBLE
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(3)))
        content = FrameLayout(this).apply { setBackgroundColor(BG) }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply { setPadding(dp(7), dp(8), dp(7), dp(12)) }
        fun add(view: TextView) {
            bottom.addView(view, LinearLayout.LayoutParams(0, dp(51), 1f).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            })
        }
        add(action("‹", "Natrag") {
            active()?.web?.let { if (it.canGoBack()) it.goBack() else showHome() }
        })
        add(action("›", "Naprijed") { active()?.web?.let { if (it.canGoForward()) it.goForward() } })
        add(action("⟳", "Osvježi") { active()?.let { if (it.home) showHome() else it.web.reload() } })
        starButton = action("☆", "Oznake") { toggleBookmark() }
        add(starButton)
        tabButton = action("1", "Kartice") { showTabs() }
        add(tabButton)
        menuButton = action("⋮", "Izbornik") { showMenu() }
        add(menuButton)
        root.addView(bottom)
        root.requestApplyInsets()
    }

    private fun active(): Tab? = tabs.getOrNull(selected)

    private fun createTab() {
        if (tabs.size >= MAX_TABS) { message("Najviše osam kartica"); return }
        val web = WebView(this)
        val tab = Tab(web)
        setupWebView(tab)
        tabs.add(tab)
        showTab(tabs.lastIndex)
    }

    private fun setupWebView(tab: Tab) {
        val web = tab.web
        web.setBackgroundColor(Color.WHITE)
        web.settings.apply {
            javaScriptEnabled = settingsPrefs.getBoolean("javascript", true)
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = true
            safeBrowsingEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setGeolocationEnabled(false)
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                return request.url.scheme?.lowercase() != "https"
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                if (url?.startsWith("https://") == true) {
                    tab.url = url
                    tab.home = false
                }
                if (active() === tab) {
                    address.setText(tab.url)
                    progress.progress = 5
                    progress.visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (url?.startsWith("https://") == true) {
                    tab.url = url
                    tab.title = view.title?.take(150)?.ifBlank { Uri.parse(url).host ?: "Web-stranica" }
                        ?: (Uri.parse(url).host ?: "Web-stranica")
                    store.addHistory(tab.title, url)
                }
                if (active() === tab) {
                    progress.visibility = View.INVISIBLE
                    address.setText(tab.url)
                    updateButtons()
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                if (active() === tab) message("SSL certifikat nije valjan")
            }

            override fun onSafeBrowsingHit(
                view: WebView, request: WebResourceRequest,
                threatType: Int, callback: SafeBrowsingResponse
            ) {
                callback.backToSafety(true)
                if (active() === tab) message("Opasna web-stranica je blokirana")
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                val index = tabs.indexOf(tab)
                if (index < 0) return true
                (view.parent as? ViewGroup)?.removeView(view)
                tabs.removeAt(index)
                view.destroy()
                if (index == selected) {
                    selected = -1
                    if (tabs.isEmpty()) createTab()
                    else showTab(index.coerceAtMost(tabs.lastIndex))
                } else {
                    if (index < selected) selected--
                    updateButtons()
                }
                message("Web-prikaz je ponovno pokrenut")
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (active() === tab && !tab.home) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress == 100) View.INVISIBLE else View.VISIBLE
                }
            }
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
        }
        web.setDownloadListener(DownloadListener { url, userAgent, disposition, mime, _ ->
            if (!url.startsWith("https://")) { message("Preuzimanje zahtijeva HTTPS"); return@DownloadListener }
            try {
                val name = File(URLUtil.guessFileName(url, disposition, mime)).name.take(120)
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setTitle(name)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    userAgent?.let { addRequestHeader("User-Agent", it) }
                    CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
                }
                (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                message("Preuzimanje je započelo")
            } catch (_: Exception) {
                message("Preuzimanje nije uspjelo")
            }
        })
    }

    private fun showTab(index: Int) {
        if (index !in tabs.indices) return
        active()?.web?.onPause()
        selected = index
        val tab = tabs[index]
        (tab.web.parent as? ViewGroup)?.removeView(tab.web)
        content.removeAllViews()
        if (tab.home) {
            content.addView(homeView(), FrameLayout.LayoutParams(-1, -1))
            address.setText("")
        } else {
            content.addView(tab.web, FrameLayout.LayoutParams(-1, -1))
            tab.web.onResume()
            address.setText(tab.url)
        }
        updateButtons()
    }

    private fun homeView(): View {
        val scroll = ScrollView(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(35), dp(24), dp(25))
        }
        scroll.addView(column)
        column.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_ghosium)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Ghosium logotip"
        }, LinearLayout.LayoutParams(-1, dp(160)))
        column.addView(text("GHOSIUM", 32f, FG, true).apply { gravity = Gravity.CENTER })
        column.addView(text("Internet po tvojim pravilima.", 14f, DIM).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(35))
        })
        val quick = EditText(this).apply {
            setSingleLine(true)
            hint = "Ghosium Search ili URL"
            setTextColor(FG)
            setHintTextColor(DIM)
            background = shape()
            setPadding(dp(16), 0, dp(16), 0)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_SEARCH) { navigate(text.toString()); true } else false
            }
        }
        column.addView(quick, LinearLayout.LayoutParams(-1, dp(56)))
        column.addView(text("Rezultate pretraživanja pruža DuckDuckGo", 12f, DIM).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        })
        column.addView(text("BRZE POVEZNICE", 12f, DIM, true).apply {
            setPadding(0, dp(35), 0, dp(14))
        })
        listOf(
            "Wikipedia" to "https://hr.wikipedia.org",
            "Android Developers" to "https://developer.android.com",
            "MDN Web Docs" to "https://developer.mozilla.org"
        ).forEach { (name, url) ->
            val tile = text("↗    " + name, 16f, FG, true).apply {
                background = shape()
                setPadding(dp(17), 0, 0, 0)
                setOnClickListener { load(url) }
            }
            column.addView(tile, LinearLayout.LayoutParams(-1, dp(53)).apply {
                bottomMargin = dp(10)
            })
        }
        return scroll
    }

    private fun showHome() {
        val tab = active() ?: return
        tab.home = true
        showTab(selected)
    }

    private fun navigate(raw: String) {
        val url = UrlResolver.resolve(raw)
        if (url == null) { message("Unesi valjanu HTTPS adresu ili upit"); return }
        load(url)
    }

    private fun load(url: String) {
        if (!url.startsWith("https://")) return
        val tab = active() ?: return
        if (tab.home) { tab.home = false; showTab(selected) }
        tab.url = url
        address.setText(url)
        tab.web.loadUrl(url)
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(address.windowToken, 0)
        address.clearFocus()
    }

    private fun updateButtons() {
        tabButton.text = tabs.size.toString()
        val tab = active()
        starButton.text = if (tab != null && !tab.home &&
            store.bookmarks.any { it.url == tab.url }) "★" else "☆"
    }

    private fun toggleBookmark() {
        val tab = active() ?: return
        if (tab.home || tab.url.isBlank()) { message("Najprije otvori stranicu"); return }
        val saved = store.toggleBookmark(tab.title, tab.url)
        message(if (saved) "Dodano u oznake" else "Uklonjeno iz oznaka")
        updateButtons()
    }

    private fun showTabs() {
        val labels = tabs.mapIndexed { index, tab ->
            (index + 1).toString() + ". " + tab.title.take(65)
        }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Kartice")
            .setItems(labels) { _, index -> showTab(index) }
            .setPositiveButton("Nova") { _, _ -> createTab() }
            .setNeutralButton("Zatvori trenutnu") { _, _ -> closeTab() }
            .setNegativeButton("Zatvori", null).show()
    }

    private fun closeTab() {
        val index = selected
        if (index !in tabs.indices) return
        val tab = tabs.removeAt(index)
        (tab.web.parent as? ViewGroup)?.removeView(tab.web)
        tab.web.stopLoading()
        tab.web.destroy()
        selected = -1
        if (tabs.isEmpty()) createTab()
        else showTab(index.coerceAtMost(tabs.lastIndex))
    }

    private fun showMenu() {
        val popup = PopupMenu(this, menuButton)
        listOf("Nova kartica", "Kartice", "Oznake", "Povijest", "Preuzimanja",
            "Postavke privatnosti", "Obriši podatke", "O Ghosiumu").forEachIndexed { idx, name ->
            popup.menu.add(0, idx, idx, name)
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> createTab()
                1 -> showTabs()
                2 -> showItems("Oznake", store.bookmarks)
                3 -> showItems("Povijest", store.history)
                4 -> try {
                    startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                } catch (_: Exception) { message("Preuzimanja nisu dostupna") }
                5 -> showPrivacySettings()
                6 -> clearData()
                7 -> AlertDialog.Builder(this).setTitle("Ghosium Browser")
                    .setMessage("Android 0.1.1 — Brendigo.\n" +
                        "Koristi Android System WebView. Nema anonimnog načina ni ugrađenog blokiranja oglasa.")
                    .setPositiveButton("U redu", null).show()
            }
            true
        }
        popup.show()
    }

    private fun showPrivacySettings() {
        val js = settingsPrefs.getBoolean("javascript", true)
        val cookies = settingsPrefs.getBoolean("cookies", true)
        val items = arrayOf(
            "JavaScript: " + if (js) "uključen" else "isključen",
            "Kolačići: " + if (cookies) "dopušteni" else "blokirani",
            "Tražilica: DuckDuckGo",
            "Obriši podatke pregledavanja"
        )
        AlertDialog.Builder(this).setTitle("Ghosium • Privatnost")
            .setItems(items) { _, index ->
                when (index) {
                    0 -> {
                        val enabled = !js
                        settingsPrefs.edit().putBoolean("javascript", enabled).apply()
                        tabs.forEach { it.web.settings.javaScriptEnabled = enabled }
                        message(if (enabled) "JavaScript uključen" else "JavaScript isključen")
                    }
                    1 -> {
                        val enabled = !cookies
                        settingsPrefs.edit().putBoolean("cookies", enabled).apply()
                        CookieManager.getInstance().setAcceptCookie(enabled)
                        if (!enabled) {
                            CookieManager.getInstance().removeAllCookies {
                                CookieManager.getInstance().flush()
                            }
                        }
                        message(if (enabled) "Kolačići dopušteni" else "Kolačići blokirani")
                    }
                    2 -> AlertDialog.Builder(this).setTitle("Tražilica")
                        .setMessage("Ghosium Search služi za unos upita. Rezultate prikazuje DuckDuckGo unutar preglednika. Oznake pružatelja pretrage ostaju dio sadržaja web-stranice.")
                        .setPositiveButton("U redu", null).show()
                    3 -> clearData()
                }
            }.setNegativeButton("Zatvori", null).show()
    }

    private fun showItems(title: String, items: List<BrowserStore.Item>) {
        if (items.isEmpty()) { message("Nema spremljenih stavki"); return }
        val copy = items.toList()
        val labels = copy.map { it.title + "\n" + it.url }.toTypedArray()
        AlertDialog.Builder(this).setTitle(title)
            .setItems(labels) { _, i -> load(copy[i].url) }
            .setNegativeButton("Zatvori", null).show()
    }

    private fun clearData() {
        AlertDialog.Builder(this).setTitle("Obriši podatke pregledavanja?")
            .setMessage("Brišu se povijest, oznake, kolačići i predmemorija. Preuzete datoteke ostaju spremljene.")
            .setPositiveButton("Obriši") { _, _ ->
                tabs.forEach {
                    (it.web.parent as? ViewGroup)?.removeView(it.web)
                    it.web.clearCache(true)
                    it.web.clearHistory()
                    it.web.destroy()
                }
                tabs.clear()
                selected = -1
                content.removeAllViews()
                store.clear()
                WebStorage.getInstance().deleteAllData()
                CookieManager.getInstance().removeAllCookies {
                    CookieManager.getInstance().flush()
                }
                createTab()
                message("Lokalni podaci su obrisani")
            }
            .setNegativeButton("Odustani", null).show()
    }

    private fun message(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()

    @Deprecated("Android navigation callback")
    override fun onBackPressed() {
        val tab = active()
        if (tab != null && tab.web.canGoBack()) tab.web.goBack()
        else if (tabs.size > 1) closeTab()
        else super.onBackPressed()
    }

    override fun onPause() { active()?.web?.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); if (active()?.home == false) active()?.web?.onResume() }
    override fun onDestroy() {
        tabs.forEach {
            (it.web.parent as? ViewGroup)?.removeView(it.web)
            it.web.destroy()
        }
        tabs.clear()
        super.onDestroy()
    }
}
