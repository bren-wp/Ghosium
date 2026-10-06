# Ghosium Windows engine integration

The active product baseline is **0.0.11**.

## Engine direction

Ghosium is migrating its Windows browser engine baseline to pinned **Tor Browser / Firefox ESR** source. The reviewed upstream contract is stored at:

```text
engine/tor-browser/windows-x64.json
```

The source verifier and bootstrap are:

```text
scripts/verify-tor-browser-upstream.ps1
scripts/bootstrap-tor-browser-source.ps1
```

The contract pins the upstream Tor Browser release, Firefox ESR version, Tor version, source archive, official source URL and SHA-256. Chromium fallback is explicitly forbidden.

## Ghosium product boundary

Ghosium owns its Brendigo/Ghosium product identity, Windows launcher, Setup/Portable distribution, profile paths, first-party links, update integration and product UI customizations.

Tor Browser, Firefox ESR, Tor and all other upstream components retain their required licenses, notices and trademark boundaries.

## Security invariants

The migration must preserve browser sandboxing, process isolation, TLS/certificate validation, Tor routing protections and the upstream anti-fingerprinting model unless a reviewed Ghosium control provides equivalent or stronger protection.

## Publication rule

The previous Chromium source pipeline must not publish Ghosium 0.0.11. Canonical publication is blocked until the Windows builder is switched to the pinned Tor Browser / Firefox ESR source baseline and passes complete runtime, Setup/Portable, signing and provenance verification.
