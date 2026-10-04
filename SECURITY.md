# Ghosium Browser Security Policy

## Supported release

Only the newest stable Ghosium Browser release is supported with security fixes.

## Windows

Each release pins `ENGINE_SOURCE_REVISION`, applies reviewed transforms and reruns full source/runtime/release verification. Browser/renderer/GPU sandboxing, site/process isolation, Safe Browsing, TLS/certificate validation, extension trust and update verification remain mandatory.

Production requires source compilation, runtime smoke, performance evidence, Setup/Portable lifecycle validation, valid Authenticode, update-manifest binding and SHA-256/provenance evidence.

## Android 0.0.4

Android targets API 36 with minimum API 29 and uses System WebView. Ghosium blocks mixed content and third-party cookies, disables direct file/content access, keeps Safe Browsing enabled, cancels SSL errors and recovers renderer termination. Host-like input is validated before navigation; external schemes require confirmation.

## Release signing

Android production uses the stable Brendigo signing identity supplied only through Actions secrets. Windows requires valid Brendigo Authenticode evidence. Private keys must not be committed.

## Reporting

Use GitHub private vulnerability reporting/Security Advisory when available. Public security page: https://ghosium.com/security
