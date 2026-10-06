# Ghosium 0.0.3 Windows Release Procedure

Ghosium 0.0.3 is the next public release after 0.0.2.

Required public assets:

```text
Ghosium-Browser-Setup.exe
Ghosium-Browser-Portable.exe
GHOSIUM-TOR-BROWSER-WINDOWS.json
SHA256SUMS.txt
```

## Candidate

1. Merge the reviewed `ghosium/0.0.3-tor-firefox-release` change to `main`.
2. Create `ghosium/release/0.0.3` from that exact approved `main` commit.
3. Add exactly `.release/ghosium-v0.0.3-preview.1.request` containing `ghosium-v0.0.3-preview.1`.
4. The release-request dispatcher runs the exact Tor Browser Windows candidate workflow.
5. Candidate evidence must match the release-branch commit and prove the Tor Browser/Firefox ESR source identity, Tor runtime identity, Setup/Portable hashes and `releaseReady=true`.
6. Open a marker-only PR from `ghosium/release/0.0.3` to `main`.
7. The marker-promotion contract blocks merge unless the exact candidate build succeeded.

## Production

After the marker reaches `main`, the production workflow dispatches the exact Tor Browser Windows build for that `main` SHA, stages the pinned Tor runtime, signs the Ghosium launcher/engine packages and publishes the immutable release.

Final public tag:

```text
ghosium-v0.0.3-preview.1
```

Release title:

```text
Ghosium 0.0.3
```

No upstream prebuilt browser package, unsigned public package, Android artifact or retired browser-engine fallback may be substituted for the canonical 0.0.3 Windows build.
