<p align="center">
  <img src="engine/branding/ghosium-mark.svg" width="112" alt="Ghosium Browser logo">
</p>

<h1 align="center">Ghosium Browser</h1>

<p align="center"><strong>Browse freely. Stay private.</strong></p>

<p align="center">
  <img alt="Release 0.0.3" src="https://img.shields.io/badge/release-0.0.3-111016">
  <img alt="Windows x64" src="https://img.shields.io/badge/platform-Windows%20x64-111016">
  <img alt="Tor Browser 15.0.24" src="https://img.shields.io/badge/Tor%20Browser-15.0.24-111016">
  <img alt="Firefox ESR 140.17.0" src="https://img.shields.io/badge/Firefox%20ESR-140.17.0-111016">
</p>

**Ghosium Browser by Brendigo** is a Windows x64 privacy browser built around a pinned Tor Browser / Firefox ESR source baseline and the Ghosium visual identity.

Version **0.0.3** continues the published Ghosium line after 0.0.1 and 0.0.2.

## Why Ghosium

Ghosium combines a familiar browser experience with a strict Tor-first runtime contract. The browser engine is built from the pinned Tor Browser 15.0.24 desktop source, based on Firefox 140.17.0 ESR, while the packaged network runtime uses the pinned Tor Expert Bundle from the Tor Project.

The public Windows application remains **Ghosium Browser**. Users launch one product through `Ghosium-Browser.exe`; the Firefox/Tor Browser engine and Tor daemon remain internal runtime components.

### Privacy by construction

- **Tor-first startup:** Ghosium starts its bundled Tor runtime before launching the browser engine.
- **Fail closed:** browser startup is blocked when the managed Tor SOCKS transport cannot be established.
- **Remote DNS through Tor:** the managed Firefox profile enables SOCKS remote DNS and disables direct proxy failover.
- **Isolated profiles:** installed and Portable builds use Ghosium-owned profile paths.
- **Pinned upstream source:** Tor Browser source and Tor runtime archives are bound to reviewed versions and SHA-256 values.
- **Release provenance:** Setup and Portable packages are tied to exact source/build evidence.

### Ghosium design stays Ghosium

The Tor Browser / Firefox ESR migration does **not** replace the product's visual identity. The existing dark Ghosium New Tab design is source-ported into the Firefox engine with the same:

- Ghosium logo and icon family
- dark `#111016` visual foundation
- mint, cyan and violet accents
- “Browse freely. Stay private.” hero
- DuckDuckGo search surface
- responsive New Tab layout
- Ghosium navigation and legal links

The canonical UI assets remain under `extension/`; the build applies them to the Firefox New Tab source before compilation.

## Windows downloads

Ghosium 0.0.3 publishes two user-facing Windows packages:

| Package | Purpose |
| --- | --- |
| `Ghosium-Browser-Setup.exe` | Standard Windows installation |
| `Ghosium-Browser-Portable.exe` | Registry-free Portable distribution with adjacent profile data |

Release assets also include SHA-256 and provenance evidence so the exact published packages can be verified.

## Engine baseline

| Component | Pinned baseline |
| --- | --- |
| Tor Browser source | 15.0.24 |
| Firefox ESR | 140.17.0esr |
| Tor | 0.4.9.13 |
| Target | Windows x86_64 |
| Public version | 0.0.3 |

The source contract is defined in `engine/tor-browser/windows-x64.json`; the Tor runtime contract is defined in `engine/tor/windows-x64.json`.

## Build integrity

The canonical Windows path is:

```text
official Tor Browser source
        ↓
SHA-256 verification
        ↓
Ghosium source branding + existing New Tab UI
        ↓
Firefox/Tor Browser Windows x64 source build
        ↓
pinned Tor Expert Bundle staging
        ↓
Ghosium-Browser.exe
        ↓
Setup + Portable
        ↓
signing + provenance + SHA-256
```

A production release is blocked if the source identity, Tor runtime, browser runtime, packaging, signing or provenance contract fails.

## Repository map

- `engine/tor-browser/` — pinned Tor Browser / Firefox ESR source contract
- `engine/tor/` — pinned Tor runtime contract
- `engine/branding/` — canonical Ghosium product identity
- `extension/` — canonical Ghosium UI and logo assets used by the Firefox source port
- `compat-preview/launcher/` — hardened public Windows launcher
- `installer/` — Setup and Portable packaging
- `scripts/` — active source-build, verification, packaging and release tooling
- `updates-web/` — fail-closed Windows update manifest/service

## Release history

- **0.0.1** — first published Ghosium preview line
- **0.0.2** — second published Ghosium preview line
- **0.0.3** — Tor Browser / Firefox ESR Windows engine migration, Tor-first runtime, preserved Ghosium UI, hardened Setup/Portable release path

## Security

Security-reducing shortcuts are not accepted to make a build pass. Certificate validation, process/browser security boundaries, package verification, Tor routing protections and fail-closed startup remain release invariants.

See `SECURITY.md` for the project security contract.

## License and third-party rights

Brendigo-authored Ghosium material is governed by the **Brendigo Proprietary Commercial Software License Agreement** in `LICENSE`.

Public repository visibility does not by itself grant an open-source license to Brendigo-authored material.

Tor Browser, Firefox ESR, Tor and every other third-party or open-source component remain governed by their own licenses and trademark terms. Required notices are preserved in `THIRD_PARTY_NOTICES.md`.
