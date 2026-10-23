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
