# Ghosium Browser Security Policy

## Supported release

Only the newest stable Ghosium Browser release is supported with security fixes.

## Windows 0.0.9 security baseline

Ghosium 0.0.9 is Windows-only and is migrating its engine baseline to pinned Tor Browser / Firefox ESR source.

The engine contract requires:

- official Tor Project source origin;
- exact version and archive pinning;
- SHA-256 verification before extraction;
- no Chromium fallback;
- preserved sandboxing and certificate validation;
- preserved Tor Browser anti-fingerprinting and route-isolation protections unless a reviewed Ghosium change explicitly replaces them with an equivalent or stronger control.

Production publication requires successful source compilation, runtime smoke tests, canonical Setup + Portable provenance, install/update/uninstall validation, valid Authenticode signatures, update-manifest binding and SHA-256 evidence.

## Release completeness

A Ghosium 0.0.9 release is complete only when the immutable release contains:

```text
Ghosium-Browser-Setup.exe
Ghosium-Browser-Portable.exe
```

and all Windows signing/provenance gates have succeeded.

## Reporting

Use the repository private vulnerability reporting / Security Advisory flow when available. Reports should include the Ghosium version, Windows version, minimal reproduction steps, expected/observed behavior and whether the issue appears specific to Ghosium-owned code.

Public security page: https://ghosium.com/security
