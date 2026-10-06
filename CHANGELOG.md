# Changelog

## 0.0.4 — Tor-only dead-code and architecture audit

- Enforces Tor Browser / Firefox ESR as the only active browser engine.
- Adds repository-wide regression guards against retired Chromium/Chrome, WebView2, CEF and Electron runtime/toolchain paths.
- Tightens dead-code hygiene while preserving required historical/legal migration wording.


## 0.0.3 — Tor Browser / Firefox ESR Windows engine

### Public release line

- Continues the published Ghosium sequence after 0.0.1 and 0.0.2.
- Windows x64 only.
- Public packages remain `Ghosium-Browser-Setup.exe` and `Ghosium-Browser-Portable.exe`.

### Engine

- Replaced the active browser-engine release path with pinned Tor Browser 15.0.24 source.
- Pinned Firefox 140.17.0 ESR and Tor 0.4.9.13.
- Disabled any release fallback to the retired engine path.
- Added hash-verified Tor Browser source bootstrap.
- Added pinned Tor Expert Bundle staging for the packaged Windows runtime.
- Added Windows cross-build requirements for `msitools`, the Rust `x86_64-pc-windows-msvc` target and the Tor Browser base-browser version contract.
- Added `NOMINMAX` to prevent Windows SDK `min`/`max` macros from breaking Firefox C++ compilation.
- Reduced build parallelism for more stable hosted builds.

### Ghosium UI and branding

- Preserved the existing Ghosium logo and icon family.
- Source-ported the existing Ghosium New Tab HTML/CSS into the Firefox/Tor Browser New Tab source before compilation.
- Preserved the existing dark design, mint/cyan/violet accents, responsive layout, navigation and DuckDuckGo search surface.
- Kept the public executable identity `Ghosium-Browser.exe`.

### Tor runtime and profiles

- The public launcher starts the managed Tor runtime before Firefox.
- Browser startup fails closed if the Tor SOCKS endpoint is unavailable.
- Firefox profile proxy settings use SOCKS5, remote DNS and no direct proxy failover.
- Installed and Portable profiles remain in Ghosium-owned paths.

### Packaging and release hardening

- Setup and Portable share the same verified source-built runtime.
- Added deterministic Windows icon generation from the existing Ghosium PNG logo frames.
- Added source/runtime/package SHA-256 and provenance evidence.
- Production publication requires Authenticode-signed Ghosium packages.
- Removed Android/mobile release paths.

### Dead-code audit

- Retired Chromium-only source rewrite, source bootstrap and source-verification tooling is removed from the active product tree.
- Active release tooling is limited to Tor Browser/Firefox source bootstrap and branding, Tor runtime staging, Windows packaging, update verification, installer smoke tests, benchmarking and store verification.
