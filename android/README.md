# Ghosium Browser for Android 0.0.4

- Package: `com.brendigo.ghosium`
- Version code/name: `4` / `0.0.4`
- Min SDK: 29; compile/target SDK: 36
- Java 17; AGP 8.13.2; Gradle 8.13
- AppCompat 1.8.0; Material 1.14.0

The app uses Android System WebView inside Ghosium-owned native UI with navigation, downloads, file chooser, fullscreen, Desktop Site, Find in Page, Share, clear browsing data, deep links, state restoration and renderer recovery.

0.0.4 validates DNS/IDN hostnames, IPv4 octets and explicit ports before host navigation. Malformed host-like input becomes a search query.

Security defaults: third-party cookies off, mixed content blocked, file/content access off, Safe Browsing on, SSL errors cancelled, external schemes confirmed, no analytics SDK, browser data excluded from cloud backup/device transfer.

`release-quality.yml` performs unit/lint/release assembly, `android-release-candidate.yml` validates the exact candidate without production keys, and `production-release.yml` verifies signed APK identity/hash/signer/source provenance.
