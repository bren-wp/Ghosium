# Ghosium Browser 0.0.9

**Ghosium Browser by Brendigo** is a Windows x64 privacy browser. The active product version is **0.0.9**.

Ghosium 0.0.9 is Windows-only. The browser engine direction is pinned to the official Tor Browser 15.0.24 desktop source, based on Firefox 140.17.0 ESR with Tor 0.4.9.13. Chromium is not an allowed fallback engine for the 0.0.9 release line.

## Product boundary

Public Windows artifacts:

- `Ghosium-Browser.exe`
- `Ghosium-Browser-Setup.exe`
- `Ghosium-Browser-Portable.exe`

The public launcher starts the internally packaged Tor Browser/Firefox runtime from `runtime/firefox.exe`. Installed and Portable profiles are isolated through Firefox's `-profile` argument. The Portable package keeps its profile in `Ghosium-Portable-Data` beside the executable.

## Tor Browser source baseline

The reviewed upstream contract lives at:

`engine/tor-browser/windows-x64.json`

It pins:

- Tor Browser: 15.0.24
- Firefox: 140.17.0 ESR
- Tor: 0.4.9.13
- target: Windows x86_64
- official Tor Project source archive and SHA-256
- Chromium fallback: disabled

`scripts/verify-tor-browser-upstream.ps1` validates that contract and `scripts/bootstrap-tor-browser-source.ps1` performs hash-verified source acquisition and extraction.

## Release safety

0.0.9 remains fail-closed until the exact Windows release candidate passes source-integrity, build, runtime, Setup, Portable, signing and provenance gates. The checked-in Windows updater manifest remains disabled until canonical publication.

No release workflow may silently substitute a Chromium build, an upstream prebuilt browser binary or an unsigned public package.

## Privacy and security

Ghosium preserves the Tor Browser anonymity model as the source baseline rather than treating Tor as only a proxy. Security-reducing shortcuts such as disabling certificate validation or browser sandbox protections are forbidden.

User-visible Ghosium branding must not imply endorsement by the Tor Project. Tor Browser, Firefox, Tor and other third-party components remain governed by their respective licenses and trademark policies.

## License and third-party rights

Brendigo-authored Ghosium material is governed by the **Brendigo Proprietary Commercial Software License Agreement** in `LICENSE`.

Public repository visibility does not by itself grant an open-source license to Brendigo-authored material.

Tor Browser, Firefox ESR, Tor and every other third-party or open-source component remain governed by their own licenses and trademark terms. Required notices are preserved in `THIRD_PARTY_NOTICES.md`.
