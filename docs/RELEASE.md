# Ghosium 0.0.11 Windows Release Procedure

The active product version is `0.0.11`.

Ghosium 0.0.11 is Windows-only. Required public assets are:

```text
Ghosium-Browser-Setup.exe
Ghosium-Browser-Portable.exe
```

## Candidate

1. Merge the reviewed 0.0.11 Windows/Tor Browser foundation to `main`.
2. Create `ghosium/release/0.0.11` from the exact approved `main` commit.
3. Add exactly `.release/ghosium-v0.0.11.request` containing `ghosium-v0.0.11`.
4. The release-request dispatcher runs the exact Tor Browser Windows candidate workflow.
5. Candidate evidence must prove the pinned Tor Browser/Firefox ESR source identity and the resulting Ghosium Windows packages.
6. Open a marker-only PR from `ghosium/release/0.0.11` to `main`.
7. The marker-promotion contract refuses merge until the exact candidate evidence is successful.

## Production

After the marker reaches `main`, the Windows production workflow dispatches the exact Tor Browser Windows build for that `main` SHA.

Production must require valid Authenticode signatures for the public Setup and Portable packages and must publish immutable provenance and SHA-256 evidence.

The final release tag is:

`ghosium-v0.0.11`

No upstream prebuilt browser package, unsigned candidate or Chromium fallback may be substituted for the canonical 0.0.11 Windows build.
