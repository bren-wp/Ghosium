# Ghosium Browser Android — QA & Security Gate

## Automated checks
- [x] Source-level Kotlin/Gradle CI task for debug APK, unsigned release AAB and unit tests is configured.
- [x] URL resolver tests covering HTTPS and untrusted schemes.
- [x] Regression tests for tab selection on close.
- [ ] Full screenshot-based parity testing against all reference designs on emulator and physical devices.
- [ ] Android UI instrumentation tests across phone sizes, navigation modes and font scaling.
- [ ] Real-device upload, crash recovery, low storage and DownloadManager tests.
- [ ] External security/privacy audit and Google Play listing review.

## Manual device scenarios
1. Fresh install: onboarding, skip/continue semantics, home and Ghosium identity.
2. Search: plain query and HTTPS domain, invalid port/host, non-HTTPS scheme, redirect and SSL errors.
3. Tabs: open eight, block ninth, switch/close first/middle/last, rotate and restore, renderer crash.
4. Bookmarks and history: add, search/filter, open, delete individual, clear data.
5. Downloads: HTTPS file download, cookie-required URL, long file names, progress, reopen Android downloads.
6. Uploads: website file picker, cancel picker, multi-file selection, rotate with picker open.
7. Settings: JavaScript/cookies on/off, startup persistence, clearing site data.
8. Adaptive UI: 320–600 dp widths, tablets, landscape, high-density icons, TalkBack, large font.
9. Safety: locked-down WebView file access, blocked mixed content, TLS error cancel, Safe Browsing, no raw JS bridge.
10. Google Play: signed AAB with company-owned upload key, target API, Data Safety, privacy policy and store metadata.

## Limitations
The UI is native; the webpage engine is Android System WebView. No isolated incognito profile or true custom search-result backend is implemented. Release without Ghosium upload credentials contains a debug APK and an unsigned AAB, clearly labelled as nonproduction.

## v0.1.3 regression additions
- [x] Pure JVM tests for accepted / rejected HTTPS URLs, unsuccessful page visit exclusion and unsafe download names.
- [x] Pure JVM tests for tab close/reselection, beyond the URL resolver tests.
- [ ] Android instrumentation: back from tabs/search/settings; verify no empty screen after activity recreation.
- [ ] On-device copy, share and find-in-page against real websites.
- [ ] Retain bookmarks while clearing history / cookies / WebStorage / cache.
- [ ] DownloadManager with filenames containing separators and invalid characters.
- [ ] Device screenshot comparisons with the requested 1:1 concepts at 320/360/412/600dp widths.
- [ ] Render process death and file chooser cancellation/rotation regressions.
- [ ] Independent security audit before declaring consumer production readiness.
