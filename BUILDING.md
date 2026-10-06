# Building Ghosium Browser 0.0.9

## Release baseline

The active product version is `0.0.9`. The only supported release target is **Windows x64**.

## Upstream engine source

The migration baseline is pinned in:

```text
engine/tor-browser/windows-x64.json
```

Before any engine build, verify the upstream contract:

```powershell
./scripts/verify-tor-browser-upstream.ps1
```

Bootstrap the exact reviewed source archive into an empty destination:

```powershell
./scripts/bootstrap-tor-browser-source.ps1 -Destination D:\src\ghosium-tor-browser
```

The bootstrap downloads from the official Tor Project archive, verifies the pinned SHA-256, rejects an incomplete source tree and writes provenance evidence. Chromium fallback is not permitted.

## Windows build contract

The canonical 0.0.9 builder must compile the pinned Tor Browser / Firefox ESR source for Windows x64, apply Ghosium-owned product branding and integration, verify the runtime, and package:

```text
Ghosium-Browser-Setup.exe
Ghosium-Browser-Portable.exe
```

The old Chromium full-source path is transitional code only and must not be used to publish Ghosium 0.0.9.

## Signing and release

Production packages require the configured Brendigo Authenticode signing identity and timestamping configuration. Signing, provenance, update-manifest binding and install/update/uninstall smoke tests must not be bypassed.

## Security boundary

Build optimization must not disable browser/renderer/GPU sandboxing, site/process isolation, TLS/certificate validation, anti-fingerprinting protections, extension/add-on trust, Tor routing safeguards or package/update signature verification.
