# Ghosium Browser Privacy

## Scope

This document covers privacy behavior controlled by Ghosium Browser on Windows and Android. Search providers and visited websites are external services.

## Product defaults

Ghosium does not operate a browser-account backend, advertising identifier system or application analytics SDK. Google Search is the default external search service and queries are sent directly to Google.

## Windows

Local browser profiles remain on device unless a website or extension intentionally sends data elsewhere. Installed profile root is `%LOCALAPPDATA%\Brendigo\Ghosium\User Data`. Portable data stays beside the Portable executable and isolated from the installed profile.

## Android

Third-party cookies are disabled, mixed content is blocked, direct file/content access is disabled, Safe Browsing is enabled and TLS errors are cancelled. External non-HTTP(S) schemes require confirmation. No application analytics/advertising SDK is included.

**Clear browsing data** clears WebView cache/history plus cookies and WebStorage managed by the app. Downloads use Android Download Manager; uploads use the system document picker.

## Store and updates

The Store source has no analytics/advertising SDK or remote font dependency. Windows updates are bound to first-party Ghosium endpoints and fail closed on identity/hash/signing mismatches.

Current public privacy policy: https://ghosium.com/legal/privacy-policy
