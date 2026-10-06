param(
  [Parameter(Mandatory = $true)]
  [string]$SourceRoot
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$expectedRevision = (Get-Content (Join-Path $repoRoot 'ENGINE_SOURCE_REVISION') -Raw).Trim()
$sourceRootResolved = (Resolve-Path $SourceRoot).Path
$actualRevision = (& git -C $sourceRootResolved rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or $actualRevision -ne $expectedRevision) {
  throw "Onion navigation hardening requires pinned Chromium $expectedRevision; found $actualRevision"
}

$target = Join-Path $sourceRootResolved 'chrome/browser/chrome_content_browser_client_navigation_throttles.cc'
if (!(Test-Path $target -PathType Leaf)) {
  throw "Pinned Chromium navigation-throttle source is missing: $target"
}

$text = [IO.File]::ReadAllText($target)
$marker = 'Ghosium Direct-mode onion navigation guard'
if ($text.Contains($marker)) {
  Write-Host 'Ghosium Direct-mode onion navigation guard already applied.'
  exit 0
}

$includeAnchor = '#include "content/public/browser/navigation_handle.h"'
if (!$text.Contains($includeAnchor)) {
  throw 'Pinned Chromium navigation-throttle include anchor changed.'
}
$text = $text.Replace(
  $includeAnchor,
  '#include "net/base/net_errors.h"' + [Environment]::NewLine +
  $includeAnchor + [Environment]::NewLine +
  '#include "content/public/browser/navigation_throttle.h"'
)

$namespaceAnchor = 'namespace {'
if (!$text.Contains($namespaceAnchor)) {
  throw 'Pinned Chromium anonymous namespace anchor changed.'
}

$guardClass = @'
// Ghosium Direct-mode onion navigation guard. Direct windows never send an
// .onion navigation to the network stack. The integrated Tor route is the only
// Ghosium mode permitted to load .onion destinations.
class GhosiumOnionNavigationThrottle : public content::NavigationThrottle {
 public:
  explicit GhosiumOnionNavigationThrottle(
      content::NavigationThrottleRegistry& registry)
      : content::NavigationThrottle(registry) {}

  ~GhosiumOnionNavigationThrottle() override = default;

  ThrottleCheckResult WillStartRequest() override { return CheckUrl(); }
  ThrottleCheckResult WillRedirectRequest() override { return CheckUrl(); }

  const char* GetNameForLogging() override {
    return "GhosiumOnionNavigationThrottle";
  }

 private:
  ThrottleCheckResult CheckUrl() {
    if (base::CommandLine::ForCurrentProcess()->HasSwitch("ghosium-tor")) {
      return PROCEED;
    }

    const GURL& url = navigation_handle()->GetURL();
    if (url.SchemeIsHTTPOrHTTPS() && url.DomainIs("onion")) {
      return ThrottleCheckResult(CANCEL, net::ERR_BLOCKED_BY_CLIENT);
    }
    return PROCEED;
  }
};

'@

$text = $text.Replace($namespaceAnchor, $namespaceAnchor + [Environment]::NewLine + [Environment]::NewLine + $guardClass)

$functionAnchor = @'
void CreateAndAddChromeThrottlesForNavigation(
    content::NavigationThrottleRegistry& registry) {
  content::NavigationHandle& handle = registry.GetNavigationHandle();
'@
if (!$text.Contains($functionAnchor)) {
  throw 'Pinned Chromium navigation-throttle registration anchor changed.'
}
$registration = @'
void CreateAndAddChromeThrottlesForNavigation(
    content::NavigationThrottleRegistry& registry) {
  content::NavigationHandle& handle = registry.GetNavigationHandle();

  // Always register the cheap guard because a normal HTTPS navigation can
  // redirect to .onion. In Tor mode it immediately proceeds.
  registry.AddThrottle(
      std::make_unique<GhosiumOnionNavigationThrottle>(registry));
'@
$text = $text.Replace($functionAnchor, $registration)

foreach ($required in @(
  $marker,
  'GhosiumOnionNavigationThrottle',
  'HasSwitch("ghosium-tor")',
  'url.DomainIs("onion")',
  'net::ERR_BLOCKED_BY_CLIENT',
  'std::make_unique<GhosiumOnionNavigationThrottle>(registry)'
)) {
  if (!$text.Contains($required)) {
    throw "Ghosium onion navigation guard lost required token: $required"
  }
}

[IO.File]::WriteAllText($target, $text, [Text.UTF8Encoding]::new($false))

$thirdPartyChanges = & git -C $sourceRootResolved status --porcelain=v1 -- third_party
if ($LASTEXITCODE -ne 0) {
  throw 'Unable to verify third_party state after onion navigation hardening.'
}
if ($thirdPartyChanges) {
  throw 'Onion navigation hardening modified third_party source.'
}

Write-Host 'Ghosium Direct-mode .onion navigation guard applied.'
