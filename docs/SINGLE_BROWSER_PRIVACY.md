# Ghosium single-browser privacy and Tor architecture

## Product rule

Ghosium is one browser product and one Windows application.

There is exactly one public browser executable:

```text
Ghosium-Browser.exe
```

There is no separate "Ghosium Tor Browser", no second browser installation, and no second branded UI. Windows distribution remains:

```text
Ghosium-Browser-Setup.exe
Ghosium-Browser-Portable.exe
```

The existing Ghosium tabs, toolbar, New Tab page, settings, colors, branding and UX remain the product UI.

## Network routing inside the same browser

Normal HTTP/HTTPS browsing and Tor browsing are capabilities of the same Ghosium application.

The browser exposes a network route state rather than launching a second browser:

- **Direct** — ordinary websites use the normal network path.
- **Tor** — browser traffic is routed through the bundled Tor client; ordinary websites and `.onion` services are both supported.

Opening a `.onion` destination must never fall back to Direct networking. If Tor is unavailable, the navigation fails closed and presents a Ghosium-owned error surface.

A route change is an in-product operation. It must not launch another branded browser executable.

## Privacy isolation

Although the user sees one browser, Direct and Tor traffic must not share identity-bearing state in a way that can correlate the two routes.

The Tor route therefore uses an isolated browser storage partition/context for at least:

- cookies;
- HTTP authentication state;
- cache;
- IndexedDB;
- local/session storage;
- service workers;
- network state;
- DNS/proxy state;
- permissions where route-specific persistence would create correlation;
- site data that can act as a stable cross-route identifier.

This is internal isolation, not a second product or second application.

## Tor transport

Tor is treated as a bundled network dependency, not as the browser UI.

The Windows build must pin the Tor source/binary input used for a release and verify its cryptographic hash before packaging. Ghosium owns process lifecycle, configuration, health checks and fail-closed routing.

The browser must never silently route a requested Tor session outside Tor because the Tor process failed.

## Google-free background network contract

Ghosium must not initiate background Google-owned product-service traffic.

This includes browser account/sync, Google telemetry/crash upload, Google variations/field-trial fetching, Google Cloud Messaging, Google-owned remote search suggestions, Google network-time queries, Google autofill crowdsourcing uploads, WebRTC diagnostic uploads and similar product-originated calls.

This rule does not block a user from deliberately navigating to Google, YouTube, Gmail or any other normal website.

The default search fallback is DuckDuckGo and remote search suggestions are disabled by default.

## Security invariants

Privacy hardening must not disable core browser security controls merely to reduce network traffic. The following remain mandatory unless replaced by an independently reviewed equivalent:

- renderer/browser sandboxing;
- site/process isolation;
- TLS certificate validation;
- certificate transparency where supported by the selected engine;
- extension trust/signature enforcement;
- signed Ghosium updates and package verification.

Any upstream service removal that also removes a security feature requires a separate reviewed replacement or an explicit security decision. It must not happen as a side effect of "de-Googling".

## UI contract

Tor controls are added to the existing Ghosium UI and must visually match the current design. No Tor Browser UI, Firefox UI or upstream Tor branding is copied into the public product.

The browser may show route state such as **Direct** or **Tor** in Ghosium styling, but it remains one browser window and one application identity.

## Release verification

A production release must prove:

1. there is only one public browser executable;
2. Setup and Portable package that same browser;
3. the bundled Tor input matches its pinned hash;
4. `.onion` cannot use Direct routing;
5. Tor failure is fail-closed;
6. Direct and Tor browser state are isolated;
7. no forbidden background Google-owned browser-service endpoint is contacted during clean-start network tests;
8. user-initiated ordinary web navigation remains functional;
9. existing Ghosium UI/UX contract is preserved.
