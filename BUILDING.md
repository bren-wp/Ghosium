# Building Ghosium Browser 0.0.11

The active product version is `0.0.11`. Ghosium is Windows x64-only.

## Upstream source

Verify the immutable upstream contract:

```powershell
./scripts/verify-tor-browser-upstream.ps1
```

Bootstrap the pinned source into an empty directory:

```powershell
./scripts/bootstrap-tor-browser-source.ps1 -Destination C:\src\ghosium-tor-browser
```

The bootstrap accepts only the official Tor Project archive pinned in `engine/tor-browser/windows-x64.json` and verifies its SHA-256 before extraction.

## Windows build direction

The canonical 0.0.11 build path is `.github/workflows/tor-browser-windows-build.yml`.

The build must produce a Windows x64 Tor Browser/Firefox-derived runtime, apply Ghosium source branding/product changes, stage the runtime under `runtime/`, compile the public `Ghosium-Browser.exe` launcher and generate Setup and Portable packages.

Firefox's supported Windows build environment and toolchain requirements apply. Cross-build or native Windows build steps must remain reproducible and version-pinned where possible.

## Profiles

The Ghosium launcher uses:

```text
-profile <Ghosium profile path>
```

It does not use Chromium's `--user-data-dir` contract.

## Production requirements

Production publication requires:

1. exact 0.0.11 source/version synchronization;
2. successful Tor Browser upstream contract verification;
3. Windows x64 source build;
4. runtime smoke validation;
5. canonical Setup and Portable packaging;
6. valid Authenticode signing;
7. package provenance and SHA-256 evidence;
8. immutable GitHub release publication from the exact promoted marker commit.

No unsigned or upstream-prebuilt browser package may be promoted as a Ghosium production release.
