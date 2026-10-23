package com.brendigo.ghosium

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
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
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.io.File

/**
 * Ghosium Android MVP.
 * Renders websites using the installed Android System WebView.
 * This implementation DOES NOT claim isolated incognito or ad blocking.
 */
class MainActivity : ComponentActivity() {
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
        var home: Boolean = true,
        var navigationFailed: Boolean = false
    )

    private lateinit var store: BrowserStore
    private lateinit var address: EditText
    private lateinit var content: FrameLayout
    private lateinit var topBar: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var tabButton: TextView
    private lateinit var menuButton: TextView
    private val tabs = mutableListOf<Tab>()
    private val screens by lazy { GhosiumScreens(this) }
    private val searchRepository by lazy { GhosiumSearchRepository() }
    private val sessionStore by lazy { TabSessionStore(this) }
    private var searchRequestId = 0
    private var panel: String? = null
    private var fileSelection: ValueCallback<Array<Uri>>? = null
    private val selectWebFiles = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val selectedFiles = if (result.resultCode == Activity.RESULT_OK) {
            data?.clipData?.let { clip ->
                Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
            } ?: data?.data?.let { arrayOf(it) }
        } else null
        fileSelection?.onReceiveValue(selectedFiles)
        fileSelection = null
    }
    private val settingsPrefs by lazy { getSharedPreferences("ghosium_settings", Context.MODE_PRIVATE) }
    private var selected = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (panel == null && active()?.home == true && tabs.size <= 1) finish()
                else navigateBack()
            }
        })
        store = BrowserStore(this)
        CookieManager.getInstance().setAcceptCookie(settingsPrefs.getBoolean("cookies", true))
        buildUi()
        val restored = if (savedInstanceState != null) {
            val urls = savedInstanceState.getStringArrayList("ghosium_urls").orEmpty()
            val homes = savedInstanceState.getBooleanArray("ghosium_homes")
            if (homes != null && urls.size == homes.size) {
                SessionPolicy.normalize(urls.mapIndexed { i, url ->
                    SessionPolicy.Entry(url, homes[i])
                }, savedInstanceState.getInt("ghosium_selected", 0))
            } else sessionStore.restore()
        } else sessionStore.restore()
        restored.entries.forEach { item ->
            val web = WebView(this)
            val tab = Tab(web, url = item.url, home = item.home)
            setupWebView(tab)
            tabs.add(tab)
        }
        showTab(restored.selected)
        if (!settingsPrefs.getBoolean("introduced", false)) showWelcome()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("ghosium_urls", ArrayList(tabs.map { it.url }))
        outState.putBooleanArray("ghosium_homes", tabs.map { it.home }.toBooleanArray())
        outState.putInt("ghosium_selected", selected.coerceAtLeast(0))
        super.onSaveInstanceState(outState)
    }

    private fun persistTabs() {
        if (tabs.isEmpty()) {
            sessionStore.clear()
        } else {
            sessionStore.save(tabs.map { SessionPolicy.Entry(it.url, it.home) }, selected)
        }
    }

    override fun onStop() {
        persistTabs()
        super.onStop()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun shape(color: Int = PANEL, radius: Int = 18): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), 0xFF30405B.toInt())
        }

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
            background = AuroraDrawable()
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
        address = EditText(this).apply {
            setSingleLine(true)
            textSize = 15f
            setTextColor(FG)
            setHintTextColor(DIM)
            hint = "⌕  Ghosium Search ili URL"
            background = shape()
            setPadding(dp(15), 0, dp(15), 0)
            setSelectAllOnFocus(true)
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) { navigate(address.text.toString()); true } else false
            }
        }
        top.addView(address, LinearLayout.LayoutParams(-1, dp(50)))
        topBar = top
        root.addView(top)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = ColorStateList.valueOf(ACCENT)
            progressBackgroundTintList = ColorStateList.valueOf(BG)
            visibility = View.INVISIBLE
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(3)))
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            setPadding(dp(7), dp(8), dp(7), dp(12))
            background = shape(0xEE0B1625.toInt(), 29)
        }
        fun add(view: TextView) {
            bottom.addView(view, LinearLayout.LayoutParams(0, dp(51), 1f).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            })
        }
        add(action("‹", "Natrag") { navigateBack() })
        add(action("›", "Naprijed") {
            if (panel != null) showTab(selected)
            active()?.let { tab -> if (!tab.home && tab.web.canGoForward()) tab.web.goForward() }
        })
        tabButton = action("1", "Kartice") { showTabs() }
        add(tabButton)
        add(action("⌂", "Početna") { showHome() })
        menuButton = action("⋮", "Izbornik") { showMenu() }
        add(menuButton)
        root.addView(bottom)
        root.requestApplyInsets()
    }

    private fun active(): Tab? = tabs.getOrNull(selected)

    private fun navigateBack() {
        if (panel != null) {
            if (panel == "welcome") settingsPrefs.edit().putBoolean("introduced", true).apply()
            showTab(selected)
            return
        }
        val tab = active() ?: return
        if (!tab.home && tab.web.canGoBack()) tab.web.goBack()
        else if (!tab.home) showHome()
        else if (tabs.size > 1) closeTab()
    }

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
                tab.navigationFailed = false
                if (url?.startsWith("https://") == true && !tab.home) {
                    tab.url = url
                    tab.home = false
                }
                if (active() === tab && panel == null && !tab.home) {
                    address.setText(tab.url)
                    progress.progress = 5
                    progress.visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (!tabs.contains(tab)) return
                if (url != null && BrowserPolicy.shouldRecordVisit(url, tab.home, tab.navigationFailed)) {
                    tab.url = url
                    tab.title = view.title?.take(150)?.ifBlank { Uri.parse(url).host ?: "Web-stranica" }
                        ?: (Uri.parse(url).host ?: "Web-stranica")
                    store.addHistory(tab.title, url)
                }
                if (active() === tab && panel == null && !tab.home) {
                    progress.visibility = View.INVISIBLE
                    address.setText(tab.url)
                    updateButtons()
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) tab.navigationFailed = true
                if (request.isForMainFrame && active() === tab && panel == null) {
                    message("Stranica se nije uspjela učitati. Provjeri mrežu i adresu.")
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                tab.navigationFailed = true
                handler.cancel()
                if (active() === tab) message("SSL certifikat nije valjan")
            }

            override fun onSafeBrowsingHit(
                view: WebView, request: WebResourceRequest,
                threatType: Int, callback: SafeBrowsingResponse
            ) {
                tab.navigationFailed = true
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
                if (active() === tab && !tab.home && panel == null) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress == 100) View.INVISIBLE else View.VISIBLE
                }
            }
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
            override fun onShowFileChooser(
                webView: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                if (fileSelection != null) {
                    callback.onReceiveValue(null)
                    return true
                }
                fileSelection = callback
                val chooser = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                        params.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                }
                return try {
                    selectWebFiles.launch(Intent.createChooser(chooser, "Odaberi datoteku"))
                    true
                } catch (_: Exception) {
                    fileSelection = null
                    callback.onReceiveValue(null)
                    false
                }
            }
        }
        web.setDownloadListener(DownloadListener { url, userAgent, disposition, mime, _ ->
            if (!BrowserPolicy.isHttpsAddress(url)) { message("Preuzimanje zahtijeva valjani HTTPS URL"); return@DownloadListener }
            try {
                val name = BrowserPolicy.safeFileName(File(URLUtil.guessFileName(url, disposition, mime)).name)
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setTitle(name)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    userAgent?.let { addRequestHeader("User-Agent", it) }
                    CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
                }
                val id = (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                val prefs = getSharedPreferences("ghosium_downloads", Context.MODE_PRIVATE)
                val ids = (prefs.getString("ids", "") ?: "").split(',').mapNotNull { it.toLongOrNull() }.takeLast(49)
                prefs.edit().putString("ids", (ids + id).joinToString(",")).apply()
                message("Preuzimanje je započelo")
            } catch (_: Exception) {
                message("Preuzimanje nije uspjelo")
            }
        })
    }

    private fun showTab(index: Int) {
        if (index !in tabs.indices) return
        panel = null
        topBar.visibility = if (tabs[index].home) View.GONE else View.VISIBLE
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
            if (tab.web.url == null && BrowserPolicy.isHttpsAddress(tab.url)) {
                tab.web.loadUrl(tab.url)
            }
        }
        updateButtons()
    }

    private fun homeView(): View = screens.home(
        onQuery = { navigate(it) },
        onMarks = { showItems("Oznake", store.bookmarks) },
        onDownloads = { showDownloads() },
        onSettings = { showPrivacySettings() },
        onHistory = { showItems("Povijest", store.history) }
    )

    private fun showPanel(name: String, view: View) {
        panel = name
        topBar.visibility = View.GONE
        active()?.web?.onPause()
        content.removeAllViews()
        (view.parent as? ViewGroup)?.removeView(view)
        content.addView(view, FrameLayout.LayoutParams(-1, -1))
        progress.visibility = View.INVISIBLE
    }

    private fun showWelcome() = showPanel("welcome", screens.welcome {
        settingsPrefs.edit().putBoolean("introduced", true).apply()
        showTab(selected)
    })

    private fun showSearch() = showPanel("search", screens.search(
        store.history.toList(), address.text.toString()) { navigate(it) }
    )

    private fun showDownloads() {
        val prefs = getSharedPreferences("ghosium_downloads", Context.MODE_PRIVATE)
        val ids = (prefs.getString("ids", "") ?: "").split(',').mapNotNull { it.toLongOrNull() }.takeLast(30)
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val records = mutableListOf<GhosiumScreens.DownloadRow>()
        for (id in ids.reversed()) {
            try {
                manager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE))
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val state = when(status) {
                            DownloadManager.STATUS_SUCCESSFUL -> "Završeno"
                            DownloadManager.STATUS_FAILED -> "Neuspješno"
                            DownloadManager.STATUS_RUNNING -> "Preuzimanje u tijeku"
                            DownloadManager.STATUS_PAUSED -> "Pauzirano"
                            else -> "Na čekanju"
                        }
                        records.add(GhosiumScreens.DownloadRow(name ?: "Datoteka", state))
                    }
                }
            } catch (_: RuntimeException) { }
        }
        showPanel("downloads", screens.downloads(records) {
            try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
            catch (_: Exception) { message("Preuzimanja nisu dostupna") }
        })
    }

    private fun showHome() {
        val tab = active() ?: return
        tab.web.stopLoading()
        tab.home = true
        showTab(selected)
    }

    private fun navigate(raw: String) {
        when (val destination = BrowserDestination.fromInput(raw)) {
            is BrowserDestination.Page -> load(destination.url)
            is BrowserDestination.Search -> showResults(destination.query)
            BrowserDestination.Invalid -> message("Unesi valjanu HTTPS adresu ili pojam za pretragu")
        }
    }

    private fun showResults(rawQuery: String) {
        val query = rawQuery.trim().take(200)
        if (query.isEmpty()) return
        val request = ++searchRequestId
        fun present(items: List<GhosiumResult>, loading: Boolean, error: String?) {
            showPanel("results", screens.searchResults(
                query, items, loading, error,
                onSearch = { navigate(it) },
                onOpen = { load(it) }
            ))
        }
        present(emptyList(), true, null)
        searchRepository.search(query) { response ->
            runOnUiThread {
                if (!isDestroyed && panel == "results" && request == searchRequestId) {
                    present(response.getOrDefault(emptyList()), false,
                        response.exceptionOrNull()?.let { "Provjeri internetsku vezu i pokušaj ponovno" })
                }
            }
        }
    }

    private fun load(url: String) {
        if (!url.startsWith("https://")) return
        val tab = active() ?: return
        if (tab.home || panel != null) { tab.home = false; showTab(selected) }
        tab.url = url
        address.setText(url)
        tab.web.loadUrl(url)
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(address.windowToken, 0)
        address.clearFocus()
    }

    private fun updateButtons() { tabButton.text = tabs.size.toString() }

    private fun toggleBookmark() {
        val tab = active() ?: return
        if (tab.home || !BrowserPolicy.isHttpsAddress(tab.url)) { message("Najprije otvori HTTPS stranicu"); return }
        val saved = store.toggleBookmark(tab.title, tab.url)
        message(if (saved) "Dodano u oznake" else "Uklonjeno iz oznaka")
        updateButtons()
    }

    private fun showTabs() {
        val entries = tabs.mapIndexed { i, tab ->
            GhosiumScreens.TabRow(tab.title, tab.url, i == selected)
        }
        showPanel("tabs", screens.tabs(entries,
            onOpen = { showTab(it) },
            onClose = { closeTabAt(it) },
            onNew = { createTab() }))
    }

    private fun closeTabAt(index: Int) {
        if (index !in tabs.indices) return
        val tab = tabs.removeAt(index)
        (tab.web.parent as? ViewGroup)?.removeView(tab.web)
        tab.web.stopLoading()
        tab.web.destroy()
        if (tabs.isEmpty()) {
            selected = -1
            createTab()
        } else {
            val next = TabSelection.afterClose(selected, index, tabs.size)
            selected = -1
            showTab(next.coerceIn(0, tabs.lastIndex))
            showTabs()
        }
    }

    private fun closeTab() = closeTabAt(selected)

    private fun showMenu() {
        val popup = PopupMenu(this, menuButton)
        listOf("Nova kartica", "Kartice", "Oznake", "Povijest", "Preuzimanja",
            "Pretraživanje", "Dodaj ili ukloni oznaku", "Osvježi stranicu",
            "Pronađi na stranici", "Kopiraj URL", "Podijeli poveznicu",
            "Postavke privatnosti", "Obriši podatke", "O Ghosiumu").forEachIndexed { idx, name ->
            popup.menu.add(0, idx, idx, name)
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> createTab()
                1 -> showTabs()
                2 -> showItems("Oznake", store.bookmarks)
                3 -> showItems("Povijest", store.history)
                4 -> showDownloads()
                5 -> showSearch()
                6 -> toggleBookmark()
                7 -> active()?.let { if (!it.home && panel == null) it.web.reload() else message("Najprije otvori stranicu") }
                8 -> findInPage()
                9 -> copyPageUrl()
                10 -> sharePageUrl()
                11 -> showPrivacySettings()
                12 -> clearData()
                13 -> AlertDialog.Builder(this).setTitle("Ghosium Browser")
                    .setMessage("Android 0.1.3 — Brendigo.\n" +
                        "Koristi Android System WebView. Nema anonimnog načina ni ugrađenog blokiranja oglasa.")
                    .setPositiveButton("U redu", null).show()
            }
            true
        }
        popup.show()
    }

    private fun currentHttps(): String? = active()?.takeIf { !it.home }
        ?.url?.takeIf { BrowserPolicy.isHttpsAddress(it) }

    private fun copyPageUrl() {
        val url = currentHttps()
        if (url == null) { message("Najprije otvori HTTPS stranicu"); return }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Ghosium URL", url))
        message("Poveznica je kopirana")
    }

    private fun sharePageUrl() {
        val url = currentHttps()
        if (url == null) { message("Najprije otvori HTTPS stranicu"); return }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        try { startActivity(Intent.createChooser(send, "Podijeli poveznicu")) }
        catch (_: Exception) { message("Dijeljenje nije dostupno") }
    }

    private fun findInPage() {
        val tab = active()
        if (tab == null || tab.home || panel != null) {
            message("Najprije otvori web-stranicu")
            return
        }
        val query = EditText(this).apply {
            setSingleLine(true)
            hint = "Pronađi tekst"
            setTextColor(FG)
            setHintTextColor(DIM)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Pronađi na stranici")
            .setView(query)
            .setPositiveButton("Sljedeće", null)
            .setNegativeButton("Zatvori", null).create()
        query.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) tab.web.clearMatches()
                else tab.web.findAllAsync(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (query.text.isNotBlank()) tab.web.findNext(true)
            }
        }
        dialog.setOnDismissListener {
            if (tabs.contains(tab)) tab.web.clearMatches()
        }
        dialog.show()
    }

    private fun showPrivacySettings() {
        val js = settingsPrefs.getBoolean("javascript", true)
        val cookies = settingsPrefs.getBoolean("cookies", true)
        showPanel("settings", screens.settings(js,cookies,
            onJs = {enabled ->
                settingsPrefs.edit().putBoolean("javascript",enabled).apply()
                tabs.forEach { it.web.settings.javaScriptEnabled = enabled }
                message(if(enabled) "JavaScript uključen" else "JavaScript isključen")
            },
            onCookies = {enabled ->
                settingsPrefs.edit().putBoolean("cookies",enabled).apply()
                CookieManager.getInstance().setAcceptCookie(enabled)
                if(!enabled) CookieManager.getInstance().removeAllCookies {
                    CookieManager.getInstance().flush()
                }
                message(if(enabled) "Kolačići dopušteni" else "Kolačići blokirani")
            },
            onClear = { clearData() },
            onInfo = { showPanel("info",screens.information()) }
        ))
    }

    private fun showItems(title: String, items: List<BrowserStore.Item>) {
        showPanel(title, screens.collection(title,items.toList(),
            onOpen = { load(it) },
            onDelete = { item ->
                if (title == "Oznake") store.toggleBookmark(item.title,item.url)
                else store.removeHistory(item.url)
                showItems(title,if(title=="Oznake")store.bookmarks else store.history)
            }
        ))
    }

    private fun clearData() {
        AlertDialog.Builder(this).setTitle("Obriši podatke pregledavanja?")
            .setMessage("Brišu se povijest, kolačići i podaci web-stranica. Oznake i preuzete datoteke ostaju spremljene.")
            .setPositiveButton("Obriši") { _, _ ->
                // Detach tabs first: callbacks from closing WebViews must not restore cleared history.
                val oldTabs = tabs.toList()
                tabs.clear()
                selected = -1
                panel = null
                fileSelection?.onReceiveValue(null)
                fileSelection = null
                content.removeAllViews()
                oldTabs.forEach {
                    (it.web.parent as? ViewGroup)?.removeView(it.web)
                    it.web.stopLoading()
                    it.web.clearCache(true)
                    it.web.clearHistory()
                    it.web.destroy()
                }
                store.clearHistory()
                sessionStore.clear()
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

    override fun onPause() { active()?.web?.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); if (panel == null && active()?.home == false) active()?.web?.onResume() }
    override fun onDestroy() {
        fileSelection?.onReceiveValue(null)
        fileSelection = null
        searchRepository.close()
        tabs.forEach {
            (it.web.parent as? ViewGroup)?.removeView(it.web)
            it.web.destroy()
        }
        tabs.clear()
        super.onDestroy()
    }
}
