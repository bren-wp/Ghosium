# Ghosium Windows engine integration

The active public release baseline is **0.0.4**.

## Engine

Ghosium 0.0.4 uses pinned **Tor Browser / Firefox ESR** source.

```text
engine/tor-browser/windows-x64.json
```

The source contract pins Tor Browser 15.0.24, Firefox 140.17.0 ESR, Tor 0.4.9.13, the official source archive and its SHA-256.

The source verifier/bootstrap are:

```text
scripts/verify-tor-browser-upstream.ps1
scripts/bootstrap-tor-browser-source.ps1
```

The Ghosium source overlay is applied by:

```text
scripts/rebrand-tor-browser-source.ps1
scripts/verify-tor-browser-branding.ps1
```

The packaged Tor runtime is staged from the pinned Tor Project contract through:

```text
engine/tor/windows-x64.json
scripts/stage-tor-runtime.ps1
```

## Ghosium product boundary

Ghosium owns its Brendigo/Ghosium product identity, Windows launcher, Setup/Portable distribution, profile paths, first-party links, updater integration and UI customizations.

The existing Ghosium New Tab design and logo are source-ported into the Firefox/Tor Browser build; the engine migration is not a visual redesign.

Tor Browser, Firefox ESR, Tor and all other upstream components retain their required licenses, notices and trademark boundaries.

## Security invariants

The build must preserve certificate validation, browser security boundaries, Tor routing protections and fail-closed startup. Direct network fallback is not accepted when the managed Tor transport is unavailable.

## Publication rule

Canonical 0.0.4 publication is allowed only from the pinned Tor Browser / Firefox ESR Windows source build with the pinned Tor runtime, successful Setup/Portable tests, release evidence and production signing.
