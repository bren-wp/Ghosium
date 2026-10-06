# Changelog

## 0.0.9 — Windows-only Tor Browser / Firefox ESR foundation

### Product scope

- Reduced Ghosium to one supported client target: Windows x64.
- Removed the retired secondary client source and its build/release workflows.
- Kept the public release contract focused on `Ghosium-Browser-Setup.exe` and `Ghosium-Browser-Portable.exe`.

### Engine migration

- Added a pinned Tor Browser desktop source contract for the reviewed Firefox ESR baseline.
- Added SHA-256 verified source bootstrap from the official Tor Project archive.
- Explicitly forbade Chromium fallback in the new engine contract.
- Blocked 0.0.9 publication until the canonical Windows builder consumes the Tor Browser / Firefox ESR source path.

### QA and release

- Added Windows-only 0.0.9 product QA.
- Added Windows-only production release orchestration.
- Kept the stable updater fail-closed before canonical publication.
- Preserved Setup/Portable lifecycle, Authenticode, provenance and SHA-256 release requirements.
