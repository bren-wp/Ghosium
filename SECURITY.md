# Ghosium Browser Security

## Windows security baseline

Ghosium 0.0.10 is Windows x64-only and uses Tor Browser 15.0.24 source, Firefox 140.17.0 ESR and Tor 0.4.9.13 as the reviewed engine baseline.

The upstream source contract is fail-closed: archive origin, version, platform and SHA-256 are pinned. Chromium fallback is forbidden for this release line.

## Privacy invariants

The project must preserve Tor Browser's privacy and anonymity defenses as the baseline rather than treating Tor as only a SOCKS proxy.

Do not weaken certificate validation, sandboxing, origin/process isolation, extension trust, update/package verification or Tor routing protections merely to make a build pass.

Profile handling must use Ghosium-owned directories. Portable profile data must stay adjacent to the Portable executable and must not fall back to the installed profile.

## Release security

Production Windows publication requires exact marker promotion, verified source build evidence, runtime smoke tests, Setup/Portable provenance, valid Authenticode signatures and SHA-256 manifests.

A failed source, runtime, signing or provenance gate blocks publication.
