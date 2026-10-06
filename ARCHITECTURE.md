# Ghosium Browser 0.0.9 Architecture

## Product scope

Ghosium 0.0.9 is a **Windows x64** browser product with one public browser identity:

```text
Ghosium-Browser.exe
Ghosium-Browser-Setup.exe
Ghosium-Browser-Portable.exe
```

## Engine boundary

The target engine baseline is the reviewed Tor Browser desktop source built on Firefox ESR. The exact upstream contract is stored in `engine/tor-browser/windows-x64.json`.

The migration contract is fail-closed:

- the upstream source archive is pinned by version, archive name and SHA-256;
- source must come from the official Tor Project archive;
- Chromium fallback is forbidden;
- production publication is blocked until the Windows builder consumes the Tor Browser / Firefox ESR source path;
- Ghosium branding remains independent from Tor Project branding;
- required upstream licenses and notices remain intact.

## Single-browser privacy model

Ghosium remains one browser application. Normal web destinations and Tor-routed destinations are capabilities of the same product, not separate branded browsers.

The privacy design must preserve route isolation, DNS/proxy fail-closed behavior, browser-state separation where required, certificate validation, browser sandboxing and Tor Browser anti-fingerprinting protections that are part of the reviewed upstream baseline.

## Profile and Portable boundary

Installed profile root:

```text
%LOCALAPPDATA%\Brendigo\Ghosium\User Data
```

Portable stores its profile beside the Portable executable through the Ghosium launcher contract. Runtime extraction is version-cached and promoted only after a ready marker proves extraction completed successfully.

## Release trust boundary

The stable updater is disabled until canonical publication. Windows production requires exact-source provenance, runtime smoke tests, Setup/Portable lifecycle tests, valid Authenticode signatures, update-manifest binding and SHA-256 evidence.

## Legal boundary

Brendigo-authored Ghosium material follows the repository product license. Tor Browser, Firefox ESR, Tor and other third-party components remain under their respective licenses and trademark terms.
