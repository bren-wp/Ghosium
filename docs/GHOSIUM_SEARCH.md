# GHOSIUM SEARCH — Brendigo-controlled results

## Architecture
Ghosium Android displays its own native search UI for a text query. Web pages are opened in Android System WebView only when a user selects a result or enters a HTTPS address. No DuckDuckGo/other search website is rendered as the search UI, and no upstream API keys are shipped in the APK.

Two supported modes:

1. **Default, limited:** no backend URL configured. Ghosium uses the official Croatian Wikipedia Action API for encyclopedic results only. Results are clearly marked Wikipedia. This is not whole-web search.
2. **General web search:** run a trusted SearXNG instance **controlled by Brendigo** and configure its HTTPS base URL at build time. The application requests `/search?format=json&language=hr&q=...`. Returned organic results render as native Ghosium cards with source attribution.

Official API docs:
- https://docs.searxng.org/dev/search_api.html
- https://docs.searxng.org/admin/installation-docker
- https://docs.searxng.org/admin/settings/settings_search.html

## Set up a Brendigo search backend (external hosting required)
1. Deploy SearXNG with its **official maintained Docker Compose template** on a Brendigo-controlled server, reverse-proxy it over HTTPS, and configure rate limiting, logging/privacy and instance security.
2. In the server's `/etc/searxng/settings.yml`, enable `search.formats` with `html` and `json`. The default often allows HTML only, in which case JSON requests return HTTP 403. Install a strong server secret and maintain the service.
3. Verify with `curl 'https://YOUR-OWN-SEARCH-HOST/search?format=json&q=android&language=hr'` that valid search results are delivered by your server.
4. Set the **GitHub repository variable** `GHOSIUM_SEARCH_BASE_URL` to `https://YOUR-OWN-SEARCH-HOST` (a full HTTPS base URL, **not** the /search path). The Android CI and GitHub Releases workflows compile the same value into the application. No credentials belong in this variable or in the APK.
5. After the service is live and configuration validated, publish a **new version**. Already-built APKs cannot obtain the new compile-time endpoint on their own.

### Security & transparency
Ghosium is the app brand and native search experience, **not** an independently crawled web index. Upstream search providers remain responsible for their data, engines may limit requests and source attribution remains visible in result cards. Inspect provider terms for a public/commercial deployment and respect trademark and copyright requirements.

The app does not send visited pages or history to the search server, only typed search queries. Like all network searches, the server sees those queries and connection metadata. Supply an accurate privacy policy and retention statement before Play publication. Do not claim zero tracking or complete anonymity without verification.

Ghosium does not provide a general search index when the server URL is unset. The user must never be redirected silently to a competing search website as a substitute.
