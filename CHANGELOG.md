# Changelog

## 0.0.8 — Single-browser privacy and integrated Tor foundation

### Windows privacy and network routing

- Kept Ghosium as one Windows browser application and one public `Ghosium-Browser.exe`; Tor is an integrated network route rather than a second browser product.
- Added a native **New Tor window** command to the existing Ghosium app menu using the same browser executable and existing UI model.
- Added a pinned Tor Expert Bundle 15.0.24 Windows x64 runtime contract with official Tor Project distribution URL and release-pinned SHA-256 verification.
- Added canonical release-stage Tor packaging and provenance under `Tor/` without changing the public Setup/Portable product identity.
- Added fail-closed Tor startup with isolated Tor user data/runtime data, SOCKS5 routing, system-DNS blocking, QUIC disablement and non-proxied WebRTC UDP blocking.
- Added Direct-mode `.onion` DNS blocking plus a navigation throttle so `.onion` requests cannot silently leave through the normal network path.
- Added Tor-process ownership binding so the bundled Tor process terminates with its owning Ghosium browser process.

### Zero-background-Google hardening

- Replaced the Google fallback search path with the built-in DuckDuckGo provider and disabled remote search suggestions by default.
- Kept Google/YouTube/Gmail available as ordinary user-initiated websites while forbidding browser-owned background Google service traffic.
- Disabled Google Cloud Messaging, Domain Reliability uploads, remote network-time queries, variations/field-trial seed fetching and experiment request headers.
- Disabled upstream crash upload, WebRTC diagnostic upload and Google Autofill crowdsourcing network requests.
- Preserved core browser security invariants including sandboxing, site/process isolation and TLS/certificate validation.

### QA, release and legal

- Added a fast single-browser privacy/Tor repository contract and integrated it into the 0.0.8 PR quality gate.
- Extended the full source verifier with Tor routing, Direct-mode onion guard and background-service hardening assertions.
- Added Tor Project attribution and updated privacy documentation for integrated Tor routing and DuckDuckGo fallback behavior.
- Advanced Windows, Android, built-in extension, Store metadata and updater metadata to product version 0.0.8.
- Added 0.0.8 Android release-candidate and production orchestration workflows while keeping the checked-in stable updater fail-closed before publication.

> 0.0.8 remains a release candidate until exact-commit source build, runtime, Setup/Portable, Android, signing and provenance gates are green.
