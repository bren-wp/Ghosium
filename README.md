# GHOSIUM Browser — Android

**Internet po tvojim pravilima.**

Nativni Android preglednik brenda Ghosium / Brendigo. Ovaj repozitorij iznova je postavljen 23. 10. 2026. kao Android projekt nakon uklanjanja starih datoteka Windows preglednika s grane main.

## MVP
- Kotlin + Android System WebView (nije neovisni Chromium fork)
- Hrvatske oznake, HTTPS navigacija, DuckDuckGo pretraživanje
- Više kartica, oznake, povijest, preuzimanja, brisanje podataka
- Sigurnosni minimum: blokirani nevaljani TLS certifikati, Safe Browsing, bez JavaScript bridgea, bez cleartext prometa, bez web dozvola za kameru/mikrofon

## Dizajn
- Brend Ghosium, midnight #090D16, mint #58F4CD, violet #9889FF
- Glavni vektorski simbol: [branding/ghosium-icon.svg](branding/ghosium-icon.svg)
- [Brand guide](branding/BRAND-GUIDE.md)
- 20 pojedinačnih PNG prikaza dizajna isporučeno je kao zaseban ZIP paket u razgovoru.

**Važno:** PNG prikazi su razvojni koncepti, ne snimke izvedenih Android ekrana. Privatne kartice, blokiranje oglasa, potpuni reader mode i ostale napredne kontrole tek treba razviti. Projekt nije potvrđen kao production-ready.

## Build
Otvoriti repozitorij u Android Studiju, uz JDK 17, Android SDK 36 i Gradle 8.13. Izgradnja: `gradle :app:testDebugUnitTest :app:assembleDebug`. Repozitorij ne sadrži neprovjereni Gradle wrapper JAR; za vlastito računalo moguće je generirati službeni wrapper.

## Licenciranje
© 2026 Brendigo. Sva prava pridržana. Ako projekt uključuje vanjske komponente, za njih vrijede pripadajuće licence. Javna dostupnost izvornog koda sama po sebi ne daje licencu za preraspodjelu autorskog materijala.

## Sigurnosne napomene
WebView i OS mogu obrađivati mrežne podatke; privatno pregledavanje i anonimnost **nisu implementirani**. Uvjeti privatnosti i proizvodni audit moraju biti dovršeni prije objave.

## Ghosium v0.1.1 — native Android and releases
Ghosium Search is a **native-branded input and browser UI**; the actual search results are delivered by DuckDuckGo's website within Android System WebView. No claim of a proprietary results index or special white-label search license is made.

Privacy controls: JavaScript toggle, cookies toggle, clear browsing data. Neither isolated incognito browsing nor tracker/ad blocking is implemented yet.

For each new version, update `VERSION`, `versionCode`, `versionName` and `docs/releases/vX.Y.Z.md`. The Android CI builds the debug APK and release AAB. After a successful build on `main`, the release workflow publishes a GitHub prerelease with **both an APK and AAB**, plus SHA-256 checksums. Duplicate tags do not get republished.

**Signing:** Without repository secrets `GHOSIUM_KEYSTORE_BASE64`, `GHOSIUM_KEYSTORE_PASSWORD`, `GHOSIUM_KEY_ALIAS`, and `GHOSIUM_KEY_PASSWORD`, the workflow labels the APK **debug** (development/test only) and AAB **unsigned** (not Play-ready). After secrets are configured, Android Gradle signs the release APK and release AAB using Ghosium's upload key. Never commit the keystore or passwords.

The release workflow only runs for a successfully tested `main` commit; all releases are prereleases pending device, security, and store-readiness QA.

## Android v0.1.2 — Ghosium native visual system
Implemented original native Android Views for Welcome, Home, Ghosium Search, Tabs, Bookmarks, History, Downloads, Settings and Security information. The browser chrome and Ghosium ghost/compass launcher art are native. Aurora backgrounds are drawn using Android Canvas (not whole-screen static screenshots), and content is scrollable on smaller phones.

Functional navigation includes actual WebView navigation, bookmark management, URL validation, tab switching and closing, individual history deletion, system file picker for website uploads, DownloadManager download status, and JavaScript/cookie toggles. Tab URLs reload after Activity recreation. DuckDuckGo still provides the external web search results page, inside Ghosium chrome.

**Scope truth:** The provided concept screenshots remain design targets, and there has not yet been a visual pixel-diff review on Android emulators across density/font scales. No full incognito profile, proprietary search engine, or complete anti-tracking engine is claimed. Android CI validates compilation, basic unit tests and AAB construction, not a complete mobile QA certification. For signed Play-ready APK/AAB, configure the Ghosium upload keystore as repository secrets, never commit it.

## Ghosium 0.1.3 — native browser hardening
- Five-button native bottom toolbar matches the design's core navigation layout. Address chrome is hidden while on home / native panels.
- Browser menu actions operate on the actual active WebView: bookmark, refresh, find in page, copy URL and share via Android.
- The **Clear browsing data** confirmation preserves bookmarks and downloaded files, deleting history, cookies, WebStorage and cached WebView data.
- Failed TLS, Safe Browsing, and main-frame network loads are omitted from recorded history.
- The shared `BrowserPolicy` enforces HTTPS and sanitizes DownloadManager filenames; JVM regression tests cover these rules.
- GitHub Actions performs Android lint, unit tests, APK and AAB builds before release. Real-device and pixel-diff screen QA remain pending.

Current prerelease remains **not Google-Play ready** without a correctly secured production upload key. GitHub emits clearly labelled debug APK and unsigned AAB in its absence. The app still does **not** provide a separate incognito browser profile.

## Ghosium v0.1.4 — Chrome-like simplicity and native results
- Compact home screen, smaller Ghosium identity, one primary search/address field and four directly accessible shortcuts, with five always-available bottom browser controls.
- Typed keywords render native Ghosium result cards without navigating to a DuckDuckGo/other search-site interface.
- With no configured own service, **only Wikipedia** results are available and are labelled as such. For broader web search a Brendigo-controlled HTTPS SearXNG backend must be deployed; see [Ghosium Search setup](docs/GHOSIUM_SEARCH.md). This is not a proprietary search index.
- A working service URL is selected via repo variable `GHOSIUM_SEARCH_BASE_URL` during CI and release builds; no upstream secrets are stored in the app.
- No promise of perfect 1:1 pixel parity or zero crashes without device screenshots and QA. APK remains debug and AAB unsigned until secured signing secrets exist.

## Ghosium v0.1.5
Ghosium Search now refreshes **result rows only**, preserving the editable native search field, its cursor and the screen's scroll position during network updates. Browser chrome uses one compact omnibox on web pages. Secondary screens no longer repeat a large decorative logo.

Up to eight HTTPS browser tabs are safely restored across restarts; background tabs are loaded only when selected. Native Ghosium error screens replace opaque network/TLS errors. Explicit `example.com:8443` HTTPS inputs are accepted.

The app still requires a genuinely Brendigo-hosted search service for broad web results. Until configured, only clearly attributed Wikipedia results are available. Device screenshot parity tests and upload signing remain prerequisites for production distribution.
