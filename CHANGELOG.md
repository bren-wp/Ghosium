# Changelog

## 0.0.9 — Windows-only Tor Browser / Firefox ESR foundation

### Product scope

- Removed the mobile application and all mobile build/release workflows.
- Ghosium is now a Windows x64-only browser product.
- Public release assets are Setup and Portable only.

### Engine migration

- Moved the active 0.0.9 engine contract away from Chromium.
- Pinned official Tor Browser 15.0.24 source.
- Pinned Firefox 140.17.0 ESR and Tor 0.4.9.13 identities.
- Added official-source SHA-256 verification and a fail-closed Tor Browser source bootstrap.
- Explicitly forbade Chromium fallback for the 0.0.9 release line.
- Began migrating the public Windows launcher to the internal Tor Browser/Firefox runtime.

### Windows launcher and profile model

- Changed the internal runtime executable contract to `runtime/firefox.exe`.
- Changed profile isolation from Chromium `--user-data-dir` to Firefox `-profile <path>`.
- Kept the public executable identity `Ghosium-Browser.exe`.
- Preserved Setup and registry-free Portable packaging as the required public Windows distribution model.

### Release safety

- Added Windows-only 0.0.9 QA and production orchestration.
- Retired mobile-bearing 0.0.8 release workflows.
- Production remains blocked until the Tor Browser-derived Windows source build, runtime, signing and provenance contracts are green.
