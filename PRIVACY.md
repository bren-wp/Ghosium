# Ghosium Browser Privacy

## Scope

This document covers privacy behavior controlled by Ghosium Browser on Windows and Android and by Ghosium-controlled product services. Search providers, Tor relays and websites intentionally opened by the user are external services.

## Product-wide defaults

Ghosium does not operate a browser-account backend, advertising identifier system or application analytics SDK. The product does not include Firebase Analytics, Google Analytics, App Center or Sentry Android telemetry.

DuckDuckGo is the built-in fallback search provider. Remote search suggestions are disabled by default. Ghosium does not operate a first-party search index or silently fall back to Google-owned search services.

## Windows

The source-built Windows product blocks third-party cookies by default and disables selected browser-owned background/reporting, search-suggestion, speculative preload and remote New Tab promotional paths covered by repository contracts. Local browser profiles remain on the device unless a website or extension intentionally sends its own data elsewhere.

Installed profile root:

```text
%LOCALAPPDATA%\Brendigo\Ghosium\User Data
```

Portable profile data stays beside the Portable executable and is not redirected into the normal installed profile.

### Integrated Tor route

Windows remains one Ghosium Browser application and one public browser executable. When the Ghosium Tor route is selected, the same browser starts the pinned bundled Tor runtime and uses an isolated Tor user-data root instead of the Direct profile.

The Tor route is configured fail-closed: browser traffic uses a local SOCKS5 endpoint, local DNS resolution is blocked for routed destinations, QUIC is disabled, non-proxied WebRTC UDP is disabled, and conflicting external proxy/PAC overrides are rejected. If the bundled Tor runtime is missing or cannot be launched, the Tor-routed browser startup fails instead of silently returning to Direct networking.

The Tor daemon is an external open-source component from The Tor Project. Tor improves network anonymity but does not make every browser action anonymous; account logins, downloads, extensions, fingerprintable behavior and information deliberately submitted to websites can still identify a user.

## Android

The Android application uses Android System WebView with Ghosium-owned navigation UI. Its defaults:

- third-party cookies disabled;
- mixed HTTP content blocked;
- direct WebView file/content access disabled;
- Safe Browsing enabled where supported;
- TLS/certificate errors cancelled rather than bypassed;
- external non-HTTP(S) schemes require user confirmation;
- no application analytics/advertising SDK;
- browser-local app data excluded from Android cloud backup and device-to-device transfer.

The app stores ordinary WebView browsing state locally on the device. Choosing **Clear browsing data** clears WebView cache/history and removes cookies and WebStorage data managed by the app/WebView.

Downloads are delegated to Android's system Download Manager after a user/site-initiated download. File upload uses the Android system document picker so the user selects which file URI to expose to the page.

## Websites and search results

In Direct browsing, a website intentionally opened by the user receives ordinary network traffic and can apply its own privacy practices subject to browser controls and Ghosium filtering. In the integrated Tor route, network traffic is routed through Tor, but website content and user actions can still reveal information.

## Ghosium Store and updates

The Store source has no analytics, advertising SDK or remote font dependency. Windows updates are bound to first-party Ghosium endpoints and fail closed on package identity, hash or signing mismatches.

Current public privacy policy: https://ghosium.com/legal/privacy-policy
