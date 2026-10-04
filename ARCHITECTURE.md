# Ghosium Browser 0.0.4 Architecture

## Product scope

Ghosium has two release clients: Windows x64 full-source Setup/Portable and Android 10+ using Android System WebView.

## Windows boundary

The repository pins exact upstream source/tool revisions and stores Ghosium branding, reviewed transforms, deterministic build configuration and release verification. Technical upstream identifiers remain where required by compatibility, APIs, build graph or legal attribution. `ghost://` and `ghost-untrusted://` remain compatibility-bound internal namespaces until a separately proven migration exists.

Installed profile root is `%LOCALAPPDATA%\Brendigo\Ghosium\User Data`. Portable uses adjacent data and a hardened private profile contract with versioned cache/staging/ready-marker semantics.

## Android boundary

Package: `com.brendigo.ghosium`. Ghosium owns native browser chrome/lifecycle; System WebView renders content. Address resolution validates DNS/IDN/IPv4/ports. Safe Browsing remains enabled, third-party cookies/mixed content/file-content access are restricted, TLS errors fail closed, external schemes require confirmation and renderer termination is recovered.

## Release trust boundary

Candidate promotion binds exact full-source evidence. Production requires Android release signing and Windows Authenticode signing. Publication is complete only when Setup, Portable and Android APK are on the same exact-commit release with verification evidence.

## Legal boundary

Brendigo-authored Ghosium material follows the product license. Third-party components retain their own licenses and attribution.
