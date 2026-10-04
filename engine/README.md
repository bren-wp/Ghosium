# Ghosium source-engine integration

The active product baseline is **0.0.4**.

This directory contains Ghosium-owned source transforms, brand assets, localization, Windows identity and deterministic build configuration for the exact Chromium source revision in `ENGINE_SOURCE_REVISION`.

## Product source contract

Ghosium owns product identity, artwork, Windows executable identity, `ghost://` / `ghost-untrusted://`, profile surfaces, New Tab branding, product links, updates and supported locales. Technical Chromium/GN symbols remain where required by build, compatibility or legal attribution.

## 0.0.4 privacy and performance

Privacy transforms and native Memory Saver/background-mode configuration remain covered by repository contracts. Security mechanisms are never disabled for benchmark results.

## Security invariants

Browser/renderer/GPU sandboxing, site/process isolation, Safe Browsing, TLS/certificate validation, extension trust and update verification are mandatory.

The canonical flow is documented in `../BUILDING.md`. Android is a separate client module and does not alter the pinned Windows engine revision.
