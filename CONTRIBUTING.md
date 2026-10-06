# Contributing to Ghosium Browser

## Scope

Ghosium 0.0.3 is a Windows x64 browser built from the pinned Tor Browser / Firefox ESR source baseline. Contributions must preserve the product's privacy, security, branding and fail-closed Tor runtime guarantees.

## Windows rules

- Ghosium-owned desktop launcher code remains C++20.
- The active engine is Tor Browser / Firefox ESR; do not reintroduce the retired browser-engine source pipeline.
- Do not add Electron, Tauri, WebView2 or another wrapper as the browser core.
- Do not disable certificate validation, browser security boundaries or Tor routing protections to make a build pass.
- Preserve the public executable identity `Ghosium-Browser.exe`.
- Preserve the existing Ghosium logo and New Tab visual identity unless a design change is explicitly requested.
- Setup/Portable changes require compilation plus lifecycle/profile tests.
- Tor runtime changes require pinned source/archive provenance and SHA-256 verification.

## Source branding and UI

The canonical Ghosium visual assets live under `extension/` and `engine/branding/`. The Tor Browser source overlay ports the existing New Tab and branding assets into the Firefox source tree before compilation.

Changes must not silently replace the existing Ghosium design with upstream browser branding.

## Web-service rules

`store-web/` and `updates-web/` target commodity shared hosting with PHP/JSON, no mandatory SQL database, no analytics/advertising SDKs, no remote-font dependency, secure headers and protected storage paths.

## Branding and third-party identity

User-facing Ghosium-owned surfaces use Ghosium branding. Required Tor Browser, Firefox ESR, Tor and other third-party legal attribution remains in designated notice/license files.

## Release assets

The canonical 0.0.3 end-user assets are:

```text
Ghosium-Browser-Setup.exe
Ghosium-Browser-Portable.exe
GHOSIUM-TOR-BROWSER-WINDOWS.json
SHA256SUMS.txt
```

A production artifact is valid only after the exact-commit source build, Tor runtime staging, Windows packaging, security, signing and provenance gates succeed.
