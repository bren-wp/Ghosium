param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$contractPath = Join-Path $repoRoot 'engine/tor-browser/windows-x64.json'

if (!(Test-Path $contractPath -PathType Leaf)) {
  throw 'Tor Browser Windows upstream contract is missing.'
}

$contract = Get-Content $contractPath -Raw | ConvertFrom-Json

if ([int]$contract.schemaVersion -ne 1) {
  throw "Unsupported Tor Browser upstream contract schema: $($contract.schemaVersion)"
}
if ([string]$contract.publisher -ne 'The Tor Project') {
  throw 'Tor Browser upstream contract publisher changed unexpectedly.'
}
if ([string]$contract.torBrowserVersion -ne '15.0.24') {
  throw 'Ghosium 0.0.9 must remain pinned to Tor Browser 15.0.24 until an explicit reviewed engine update.'
}
if ([string]$contract.firefoxVersion -ne '140.17.0esr') {
  throw 'Pinned Firefox ESR version does not match the reviewed Tor Browser desktop baseline.'
}
if ([string]$contract.torVersion -ne '0.4.9.13') {
  throw 'Pinned Tor version does not match the reviewed Tor Browser release baseline.'
}
if ([string]$contract.targetPlatform -ne 'windows-x86_64') {
  throw 'Ghosium engine target must remain Windows x86_64.'
}
if ([bool]$contract.chromiumFallbackAllowed) {
  throw 'Chromium fallback is forbidden by the Ghosium 0.0.9 engine contract.'
}
if ([string]$contract.sourceUrl -notmatch '^https://archive\.torproject\.org/tor-package-archive/torbrowser/15\.0\.24/') {
  throw 'Tor Browser source must come from the official Tor Project archive.'
}
if ([string]$contract.signatureUrl -ne ([string]$contract.sourceUrl + '.asc')) {
  throw 'Tor Browser source signature URL is not bound to the pinned archive URL.'
}
if ([string]$contract.sha256 -ne 'c217a69a1c929a81d5217247eb10dc5306176d94b2ab7918ddd9e4c5a28e169f') {
  throw 'Pinned Tor Browser source SHA-256 changed without review.'
}
if ([string]$contract.sourceArchive -ne 'src-firefox-tor-browser-140.17.0esr-15.0-1-build4.tar.xz') {
  throw 'Pinned Tor Browser source archive name changed unexpectedly.'
}
if ([string]$contract.buildSystem.repository -ne 'https://gitlab.torproject.org/tpo/applications/tor-browser-build.git' -or
    [string]$contract.buildSystem.branch -ne 'maint-15.0' -or
    [string]$contract.buildSystem.commit -ne '137f3dcdb78a58f157c1f7e991463c600f630728' -or
    [string]$contract.buildSystem.target -ne 'torbrowser-release-windows-x86_64') {
  throw 'Pinned Tor Browser Windows build-system contract changed without review.'
}

Write-Host 'Ghosium Tor Browser/Firefox ESR Windows upstream contract: OK'
