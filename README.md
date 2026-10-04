<p align="center">
  <img src="engine/branding/ghosium-mark.svg" width="112" alt="Ghosium Browser icon">
</p>

# Ghosium Browser 0.0.4

**Ghosium Browser by Brendigo** is a privacy-focused browser for **Windows x64** and **Android 10+**. The active product version is **0.0.4**.

0.0.4 retains the pinned full-source Windows architecture and hardens Android navigation, release automation and repository hygiene. Publication remains fail-closed.

## Product identity

- Product: **Ghosium Browser**
- Publisher: **Brendigo**
- Windows: x64 Setup and registry-free Portable
- Android: `com.brendigo.ghosium`, API 29+, target API 36
- Home: https://ghosium.com/
- Store: https://store.ghosium.com/
- Updates: https://updates.ghosium.com/
- Security: https://ghosium.com/security
- Privacy: https://ghosium.com/legal/privacy-policy
- Default external search: Google Search

## Windows

Windows is compiled from the exact pinned upstream engine revision in `ENGINE_SOURCE_REVISION` using `DEPOT_TOOLS_REVISION`. Reviewed transforms apply Ghosium/Brendigo identity and product behavior while preserving sandboxing, site/process isolation, Safe Browsing and TLS/certificate validation.

Installed profile root is `%LOCALAPPDATA%\Brendigo\Ghosium\User Data`. Portable uses its own adjacent profile and versioned runtime cache. Production Setup and Portable packages require real runtime/lifecycle smoke, Authenticode verification and provenance.

## Android

The Android client uses Android System WebView in a Ghosium-owned shell with navigation, downloads, file chooser, fullscreen media, Desktop Site, Find in Page, Share, local data cleanup, lifecycle restoration and renderer recovery.

0.0.4 validates DNS labels, IDN domains, IPv4 octets and port ranges before treating host-like input as navigation. Malformed host-like input falls back to search. AndroidX AppCompat is updated to stable 1.8.0 while API 36, AGP 8.13.2, Gradle 8.13 and Material 1.14.0 remain pinned.

## Release assets

A complete production release requires:

```text
Ghosium-Browser-Setup.exe
Ghosium-Browser-Portable.exe
Ghosium-Browser-Android.apk
```

Unsigned QA artifacts are never production artifacts.

## Development and verification

- QA: `.github/workflows/release-quality.yml`
- Android candidate: `.github/workflows/android-release-candidate.yml`
- Candidate dispatcher: `.github/workflows/release-request-dispatch.yml`
- Candidate promotion: `.github/workflows/release-marker-promotion-contract.yml`
- Windows full-source build: `.github/workflows/full-source-windows-build.yml`
- Production: `.github/workflows/production-release.yml`
- Build: `BUILDING.md`
- Release procedure: `docs/RELEASE.md`
- Architecture: `ARCHITECTURE.md`
- Privacy: `PRIVACY.md`
- Security: `SECURITY.md`

The checked-in Windows update manifest stays fail-closed until canonical publication.

## License and third-party rights

Brendigo-authored Ghosium material is governed by the **Brendigo Proprietary Commercial Software License Agreement** in `LICENSE`. Public repository visibility does not by itself grant an open-source license to Brendigo-authored proprietary material.

Chromium and every other third-party or open-source component remain governed by their own licenses. Required attribution is preserved in `THIRD_PARTY_NOTICES.md`.
