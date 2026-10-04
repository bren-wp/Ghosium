# Changelog

## 0.0.4 — production hardening

### Android
- Hardened DNS/IDN/IPv4/port address classification and added regression tests.
- Malformed host-like input now falls back to search.
- Updated AndroidX AppCompat to 1.8.0 while retaining API 36, AGP 8.13.2, Gradle 8.13, Java 17 and Material 1.14.0.
- Preserved Safe Browsing, TLS fail-closed behavior, mixed-content blocking, third-party-cookie blocking and WebView file/content restrictions.

### Release engineering
- Replaced version-specific QA/Android/production orchestration with VERSION-bound generic workflows.
- Removed obsolete one-time release workflows and stale release markers.
- Preserved exact-candidate evidence, full-source Windows build, signing, provenance and final asset verification.

### Repository and legal hygiene
- Updated active documentation, extension/store metadata and fail-closed update metadata to 0.0.4.
- Added hygiene enforcement preventing obsolete version-specific workflow return.
- Removed legal placeholders without inventing corporate registration data.

> 0.0.4 is not complete until exact candidate, marker promotion, signed Android, signed full-source Windows and immutable release verification succeed.
