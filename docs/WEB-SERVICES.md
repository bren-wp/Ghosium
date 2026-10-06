# Ghosium web services deployment note

Ghosium Browser 0.0.8 does not ship or operate a first-party web search service. The built-in fallback search provider is DuckDuckGo, remote search suggestions are disabled by default, and Ghosium does not silently route searches through a Google-owned browser service.

The repository keeps only the Ghosium-controlled shared-hosting services needed for product distribution:

- Store source: `store-web/`
- Update source: `updates-web/`
- Store deployment guide: `docs/SHARED-HOSTING-STORE.md`

The Windows browser additionally bundles a pinned Tor Expert Bundle as a local network transport dependency. Tor relay traffic is not a Ghosium-hosted web service.

Retired Ghosium search server integration and its deployment assets are intentionally absent. User-initiated visits to DuckDuckGo, Google, YouTube, Gmail and other websites remain ordinary external web traffic.
