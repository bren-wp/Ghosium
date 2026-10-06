<p align="center">
  <img src="engine/branding/ghosium-mark.svg" width="112" alt="Ghosium Browser icon">
</p>

# Ghosium Browser 0.0.9

**Ghosium Browser by Brendigo** is a privacy-focused **Windows x64** browser. The active product version is **0.0.9**.

The active development direction is a Windows-only migration from the previous Chromium engine path to a pinned **Tor Browser / Firefox ESR** source baseline. Publication is fail-closed: Ghosium 0.0.9 must not be released until the Tor Browser-based Windows builder, runtime verification, Setup/Portable packaging, Authenticode signing and provenance gates are complete.

## Product identity

- Product: **Ghosium Browser**
- Publisher: **Brendigo**
- Platform: **Windows x64**
- Public browser: `Ghosium-Browser.exe`
- Distribution: `Ghosium-Browser-Setup.exe` and `Ghosium-Browser-Portable.exe`
- Home: `https://ghosium.com/`
- Store: `https://store.ghosium.com/`
- Updates: `https://updates.ghosium.com/`
- Privacy: `https://ghosium.com/legal/privacy-policy`
- Default external search provider: DuckDuckGo

## Tor Browser / Firefox ESR engine direction

`engine/tor-browser/windows-x64.json` pins the reviewed upstream source baseline. The bootstrap path verifies the official source archive SHA-256 before extraction and explicitly forbids a Chromium fallback.

Ghosium remains one browser product and preserves its own Brendigo/Ghosium identity. Upstream third-party source, licenses and required notices remain attributed to their original owners.

## Windows profile and packaging

Installed profile root:

```text
%LOCALAPPDATA%\Brendigo\Ghosium\User Data
```

Portable keeps browser data beside the Portable executable, uses versioned runtime caching and does not create shortcuts or registry-backed profile state.

The checked-in stable update manifest remains fail-closed before canonical publication: `enabled:false`, empty SHA-256 and zero size.

## Development and verification

- Windows 0.0.9 QA: `.github/workflows/ghosium-0.0.9-quality.yml`
- Windows production orchestration: `.github/workflows/ghosium-0.0.9-production-release.yml`
- Tor Browser upstream verifier: `scripts/verify-tor-browser-upstream.ps1`
- Tor Browser source bootstrap: `scripts/bootstrap-tor-browser-source.ps1`
- Build documentation: `BUILDING.md`
- Release procedure: `docs/RELEASE.md`
- Architecture: `ARCHITECTURE.md`
- Security: `SECURITY.md`

## License and third-party rights

Brendigo-authored Ghosium material is governed by the **Brendigo Proprietary Commercial Software License Agreement** in `LICENSE`. Tor Browser, Firefox ESR, Tor and every other third-party or open-source component remain governed by their own licenses and trademark terms. Required attribution is preserved in `THIRD_PARTY_NOTICES.md` and applicable bundled material.
