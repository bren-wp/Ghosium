# Building Ghosium Browser 0.0.4

## Release baseline

The active product version is `0.0.4`. Windows and Android production artifacts must come from the exact same Git commit.

## Windows canonical build

Windows x64 is built from the exact Chromium source revision in `ENGINE_SOURCE_REVISION` with the exact `DEPOT_TOOLS_REVISION`. Production cannot use floating branches or precompiled snapshots.

`.github/workflows/full-source-windows-build.yml` performs source/toolchain preflight, patch-anchor verification, pinned source bootstrap, Ghosium transforms, deterministic configuration, full compilation, runtime/security verification, performance evidence, canonical Setup/Portable packaging, lifecycle smoke, production Authenticode signing, update-manifest binding, provenance and SHA-256 generation.

## Android

```text
applicationId  com.brendigo.ghosium
minSdk         29
compileSdk     36
targetSdk      36
versionCode    4
versionName    0.0.4
JDK            17
AGP            8.13.2
Gradle         8.13
AppCompat      1.8.0
Material       1.14.0
```

Equivalent verification:

```text
gradle --no-daemon -p android testDebugUnitTest lintDebug lintRelease assembleDebug assembleRelease
```

Gradle 8.13 is accepted only after SHA-256 `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78` matches.

## Signing

Android production uses only GitHub Actions secrets `GHOSIUM_ANDROID_KEYSTORE_BASE64`, `GHOSIUM_ANDROID_KEYSTORE_PASSWORD`, `GHOSIUM_ANDROID_KEY_ALIAS`, `GHOSIUM_ANDROID_KEY_PASSWORD`. Windows production requires the controlled source builder and Brendigo Authenticode identity. Private signing keys must never be committed.

## Production orchestration

`.github/workflows/production-release.yml` derives version/tag/artifact identities from repository VERSION, requires the exact marker, signs Android first, runs exact-main full-source Windows production, then verifies the immutable release.

## Security boundary

No release fix may disable sandboxing, Safe Browsing, TLS/certificate validation, site/process isolation, extension verification or update signature verification.
