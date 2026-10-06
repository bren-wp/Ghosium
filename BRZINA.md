# Ghosium Browser 0.0.3 — performance policy

Ghosium 0.0.3 changes the browser engine to the pinned Tor Browser / Firefox ESR Windows source baseline. Historical performance evidence from older engine generations is retained only for provenance and must **not** be presented as a 0.0.3 performance result.

## What may be claimed

A numerical speed, RAM, CPU, GPU or startup claim for Ghosium 0.0.3 requires a measurement produced from the exact public 0.0.3 build on a documented Windows host.

Until such evidence exists, the project makes no universal claim that 0.0.3 is faster or lighter than another browser.

## Measurement rules

1. Record the exact Ghosium release SHA and package SHA-256.
2. Use isolated Ghosium profiles and do not terminate unrelated user browser processes.
3. Record Windows version, hardware/runner identity and test methodology.
4. Keep Tor enabled when measuring the public Ghosium runtime; do not benchmark a security-weakened configuration as the product.
5. Do not disable certificate validation or browser security boundaries for performance.
6. Separate browser startup timing from network latency measurements.
7. Preserve raw evidence alongside any summary.

## Historical baseline

`benchmarks/windows/v0.8.0-hosted-baseline.json` remains immutable historical evidence for its original build only. It is not a 0.0.3 benchmark and is not used to advertise the current Tor Browser / Firefox ESR engine.
