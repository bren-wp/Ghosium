param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$productPath = Join-Path $repoRoot 'engine/branding/product.json'
$torPath = Join-Path $repoRoot 'engine/tor/windows-x64.json'
$torRewritePath = Join-Path $repoRoot 'scripts/rewrite-engine-tor-route.ps1'
$onionGuardPath = Join-Path $repoRoot 'scripts/rewrite-engine-onion-guard.ps1'
$searchRewritePath = Join-Path $repoRoot 'scripts/rewrite-engine-default-search.ps1'
$stageTorPath = Join-Path $repoRoot 'scripts/stage-tor-runtime.ps1'
$googleServicesPath = Join-Path $repoRoot 'scripts/rewrite-engine-disable-google-services.ps1'
$architecturePath = Join-Path $repoRoot 'docs/SINGLE_BROWSER_PRIVACY.md'
$applyPipelinePath = Join-Path $repoRoot 'scripts/apply-engine-branding.ps1'
$upstreamActionsPath = Join-Path $repoRoot 'scripts/rewrite-engine-upstream-public-actions.ps1'

foreach ($required in @(
  $productPath,
  $torPath,
  $torRewritePath,
  $onionGuardPath,
  $searchRewritePath,
  $stageTorPath,
  $googleServicesPath,
  $architecturePath,
  $applyPipelinePath,
  $upstreamActionsPath
)) {
  if (!(Test-Path $required -PathType Leaf)) {
    throw "Single-browser privacy contract file is missing: $required"
  }
}

$product = Get-Content $productPath -Raw | ConvertFrom-Json
if ([string]$product.externalServices.defaultSearch.name -ne 'DuckDuckGo' -or
    [string]$product.externalServices.defaultSearch.searchUrl -notmatch '^https://duckduckgo\.com/') {
  throw 'Ghosium privacy baseline requires DuckDuckGo as the built-in fallback search provider.'
}
if ([bool]$product.externalServices.defaultSearch.suggestionsEnabledByDefault) {
  throw 'Remote search suggestions must remain disabled by default.'
}

$privacy = $product.privacyBaseline
foreach ($disabled in @(
  'internalGoogleWebServices',
  'browserAccountAndSync',
  'telemetryUpload',
  'crashUpload',
  'remoteSearchSuggestions',
  'speculativePreloading',
  'alternateErrorPageService'
)) {
  if ([string]$privacy.$disabled -ne 'disabled') {
    throw "Ghosium zero-background-Google privacy control regressed: $disabled"
  }
}
if ([string]$privacy.thirdPartyCookies -ne 'blocked' -or
    [string]$privacy.userInitiatedGoogleNavigation -ne 'allowed') {
  throw 'Ghosium privacy baseline must block third-party cookies while preserving deliberate user navigation.'
}

$tor = Get-Content $torPath -Raw | ConvertFrom-Json
if ([int]$tor.schemaVersion -ne 1 -or
    [string]$tor.publisher -ne 'The Tor Project' -or
    [string]$tor.platform -ne 'windows-x86_64' -or
    [string]$tor.sha256 -notmatch '^[0-9a-f]{64}$') {
  throw 'Pinned Tor runtime metadata is invalid.'
}
if ([string]$tor.socksEndpoint -ne '127.0.0.1:17650' -or
    [string]$tor.controlEndpoint -ne '127.0.0.1:17651') {
  throw 'Ghosium-specific Tor loopback endpoints changed without review.'
}
if ([string]$tor.url -notmatch '^https://dist\.torproject\.org/') {
  throw 'Tor runtime must be fetched from the pinned official Tor Project distribution endpoint.'
}

$torRewrite = Get-Content $torRewritePath -Raw
foreach ($requiredToken in @(
  'kGhosiumTorSwitch[] = "ghosium-tor"',
  'socks5://$socksEndpoint',
  'host-resolver-rules',
  'direct_resolver_rules',
  'caller_rules',
  'rejects external proxy/DNS overrides',
  'MAP *.onion ~NOTFOUND',
  'disable-quic',
  'disable-background-networking',
  'disable_non_proxied_udp',
  'Tor User Data',
  'Tor Runtime Data',
  '__OwningControllerProcess'
)) {
  if (!$torRewrite.Contains($requiredToken)) {
    throw "Ghosium Tor source transform lost required privacy token: $requiredToken"
  }
}

$googleServices = Get-Content $googleServicesPath -Raw
foreach ($requiredToken in @(
  'Google Cloud Messaging is not a browser dependency',
  'never create the background Domain Reliability uploader',
  'never query a browser-owned remote network-time service',
  'no remote variations/field-trial seed fetching',
  'never attach experiment identifiers to web requests',
  'crash data is never uploaded to an upstream endpoint',
  'WebRTC diagnostic data stays off upstream upload paths',
  'kWebRtcTextLogCollectionAllowed, false',
  'do not query or upload form structure to Google Autofill'
)) {
  if (!$googleServices.Contains($requiredToken)) {
    throw "Ghosium Google-service hardening lost required source contract: $requiredToken"
  }
}

$onionGuard = Get-Content $onionGuardPath -Raw
foreach ($requiredToken in @(
  'Ghosium Direct-mode onion navigation guard',
  'GhosiumOnionNavigationThrottle',
  'HasSwitch("ghosium-tor")',
  'url.DomainIs("onion")',
  'net::ERR_BLOCKED_BY_CLIENT'
)) {
  if (!$onionGuard.Contains($requiredToken)) {
    throw "Ghosium Direct-mode onion guard lost required contract: $requiredToken"
  }
}

$searchRewrite = Get-Content $searchRewritePath -Raw
if (!$searchRewrite.Contains('PrepopulatedEngineToTemplateURLData(&duckduckgo)') -or
    !$searchRewrite.Contains('Google-owned fallback behavior remains active')) {
  throw 'Ghosium default-search transform no longer fail-closes against Google fallback.'
}

$stageTor = Get-Content $stageTorPath -Raw
foreach ($requiredToken in @(
  'Get-FileHash $Path -Algorithm SHA256',
  'Downloaded Tor Expert Bundle failed the pinned SHA-256 contract.',
  'Get-ChildItem $extractRoot -File -Recurse -Filter ''tor.exe''',
  'GHOSIUM-TOR-RUNTIME.json'
)) {
  if (!$stageTor.Contains($requiredToken)) {
    throw "Ghosium Tor packaging lost verification token: $requiredToken"
  }
}

$applyPipeline = Get-Content $applyPipelinePath -Raw
foreach ($requiredTransform in @(
  'rewrite-engine-browser-signin.ps1',
  'rewrite-engine-profile-picker-local-only.ps1',
  'rewrite-engine-local-profile-surfaces.ps1',
  'rewrite-engine-app-menu-account-surfaces.ps1',
  'rewrite-engine-disable-unowned-promos.ps1',
  'rewrite-engine-upstream-public-actions.ps1',
  'rewrite-engine-disable-google-services.ps1',
  'rewrite-engine-tor-route.ps1',
  'rewrite-engine-onion-guard.ps1',
  'rewrite-engine-tor-menu.ps1'
)) {
  if (!$applyPipeline.Contains($requiredTransform)) {
    throw "Canonical Ghosium source pipeline lost privacy transform: $requiredTransform"
  }
}

$upstreamActions = Get-Content $upstreamActionsPath -Raw
foreach ($requiredAiSuppression in @(
  'Google GEIC/Gemini side panel',
  'Google Glic/Gemini side panel',
  'Google AI overlay toolbar action'
)) {
  if (!$upstreamActions.Contains($requiredAiSuppression)) {
    throw "Ghosium Google AI surface hardening is incomplete: $requiredAiSuppression"
  }
}
if ($upstreamActions.Contains("-Description 'Customize Chromium side panel'")) {
  throw 'Ghosium Customize must remain available while Google AI surfaces are suppressed.'
}

$architecture = Get-Content $architecturePath -Raw
foreach ($requiredText in @(
  'There is exactly one public browser executable',
  'Ghosium-Browser.exe',
  'no separate "Ghosium Tor Browser"',
  'must never fall back to Direct networking',
  'background Google-owned product-service traffic'
)) {
  if (!$architecture.Contains($requiredText)) {
    throw "Single-browser privacy architecture lost required contract text: $requiredText"
  }
}

Write-Host 'Ghosium single-browser privacy/Tor repository contract: OK'
