# Ghosium 0.0.9 Release Procedure

The active product version is `0.0.9`.

## Release state

Ghosium Browser 0.0.9 is a **Windows-only release candidate** until the Tor Browser / Firefox ESR engine migration, exact-source build, runtime, signing, provenance and publication gates all succeed.

The checked-in stable update baseline remains fail-closed before publication.

## Required end-user assets

```text
Ghosium-Browser-Setup.exe
Ghosium-Browser-Portable.exe
```

QA stubs, compatibility engines, unsigned candidate binaries and Chromium-built browser packages must not be renamed or promoted as the 0.0.9 production release.

## Phase 1 — engine baseline

1. Verify `VERSION` and the Tor Browser upstream contract.
2. Bootstrap the exact reviewed Tor Browser / Firefox ESR source with SHA-256 verification.
3. Apply Ghosium branding/integration without replacing required upstream licenses or security controls.
4. Compile the Windows x64 browser from that source.
5. Verify runtime identity, Tor routing, normal web navigation, route isolation and anti-fingerprinting baseline.
6. Produce canonical Setup and Portable packages and run lifecycle smoke tests.

## Phase 2 — exact candidate

1. Create `ghosium/release/0.0.9` from the exact approved `main` baseline.
2. Add exactly `.release/ghosium-v0.0.9.request` containing `ghosium-v0.0.9`.
3. Run the Windows candidate workflow against that exact release-branch SHA.
4. Require source, runtime, Setup/Portable, provenance and SHA-256 evidence.
5. Promote the marker only after exact candidate evidence is green.

## Phase 3 — production

The marker push to `main` triggers `.github/workflows/ghosium-0.0.9-production-release.yml`.

Production is fail-closed and must require:

1. exact version + marker validation;
2. exact Tor Browser / Firefox ESR source provenance;
3. canonical signed Windows build;
4. runtime and installer smoke tests;
5. valid Authenticode Setup and Portable signatures;
6. update-manifest binding;
7. immutable GitHub release containing the required Windows assets.

## Release decision

Publication is forbidden while the production builder still depends on the Chromium engine path. The release may be published only after the canonical builder has been converted to the pinned Tor Browser / Firefox ESR baseline and all Windows release gates are green.
