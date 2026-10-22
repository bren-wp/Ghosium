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
