# Ghosium Browser 0.0.11 Architecture

Ghosium 0.0.11 is a **Windows x64-only** browser product.

## Engine boundary

The active browser-engine baseline is Tor Browser 15.0.24 source, based on Firefox 140.17.0 ESR and Tor 0.4.9.13.

The engine contract is stored in `engine/tor-browser/windows-x64.json`. It pins the official Tor Project source archive, archive SHA-256 and target platform. Chromium fallback is explicitly forbidden for the 0.0.11 release line.

The repository does not vendor the full Tor Browser source tree. It stores Ghosium-owned build, branding, packaging, verification and release tooling plus the immutable upstream contract.

## Windows process layout

The public executable is `Ghosium-Browser.exe`.

The launcher resolves the packaged engine at:

`runtime/firefox.exe`

The internal upstream executable name is an implementation detail. It is not a second public browser product.

Installed profile data is stored under the Brendigo/Ghosium application-data namespace. Portable profile data is stored beside the Portable executable in `Ghosium-Portable-Data`.

The launcher passes the selected profile through Firefox's `-profile <path>` interface and blocks caller attempts to override protected profile/debugging arguments.

## Packaging

The two public distribution packages are:

- `Ghosium-Browser-Setup.exe`
- `Ghosium-Browser-Portable.exe`

Both are generated from the same verified runtime stage and exact source identity. Production publication requires Authenticode signing, provenance evidence and SHA-256 manifests.

## Privacy model

Ghosium uses Tor Browser source as the privacy/anonymity baseline. The objective is to preserve Tor Browser-class first-party isolation and fingerprint-resistance behavior while applying Ghosium branding and product integration.

The release process must fail closed if the expected Tor Browser source identity, Firefox ESR identity, Tor identity or Windows target changes without review.
