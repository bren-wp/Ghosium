# Ghosium 0.0.4 Release Procedure

The active product version is `0.0.4` and remains a release candidate until all exact-commit, signing, provenance and publication gates succeed.

## Phase 1 — baseline

Develop on a version-advance branch without a production marker. Require normal contracts and `release-quality.yml`. Merge only with relevant green checks.

## Phase 2 — candidate

Create `ghosium/release/0.0.4` from approved `main`. Add only `.release/ghosium-v0.0.4.request` containing `ghosium-v0.0.4`. Require the generic Windows candidate dispatcher, Android release-candidate workflow and marker-promotion evidence contract. Merge only the marker after those checks succeed.

## Phase 3 — production

The marker push to `main` triggers `production-release.yml`:

1. validate exact VERSION + single marker;
2. sign/test/lint/minify/verify Android;
3. dispatch canonical exact-main full-source Windows production;
4. require compile/runtime/performance/Setup/Portable/signing/provenance publication;
5. attach verified Android APK/provenance;
6. verify required release assets are non-empty.

Missing signing identity, failed QA or incomplete artifacts block release.
