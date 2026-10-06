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
  throw "Refusing to rewrite default search on an unpinned checkout. Expected $expectedRevision; found $actualRevision"
}

$target = Join-Path $sourceRootResolved 'components/search_engines/template_url_prepopulate_data.cc'
if (!(Test-Path $target -PathType Leaf)) {
  throw "Pinned Chromium search-engine source is missing: $target"
}

$text = [IO.File]::ReadAllText($target)
$functionPattern = '(?ms)std::unique_ptr<TemplateURLData>\s+GetPrepopulatedFallbackSearch\s*\((?<parameters>.*?)\)\s*\{(?<body>.*?)\n\}'
$functionMatch = [regex]::Match($text, $functionPattern)
if (!$functionMatch.Success) {
  throw 'Pinned Chromium fallback-search function shape changed; refusing an unreviewed default-search modification.'
}

$parameters = $functionMatch.Groups['parameters'].Value
foreach ($requiredParameter in @('PrefService& prefs', 'regional_prepopulated_engines')) {
  if (!$parameters.Contains($requiredParameter)) {
    throw "Pinned Chromium fallback-search parameters changed; missing reviewed token: $requiredParameter"
  }
}

$body = [regex]::Replace($functionMatch.Groups['body'].Value, '\s+', ' ').Trim()
$alreadyHardened = $body.Contains('PrepopulatedEngineToTemplateURLData(&duckduckgo);')
$upstreamGoogle = $body.Contains('FindPrepopulatedEngineInternal') -and $body.Contains('google.id') -and $body.Contains('/*use_first_as_fallback=*/true')

if ($upstreamGoogle) {
  $replacement = @'
std::unique_ptr<TemplateURLData> GetPrepopulatedFallbackSearch(
    PrefService& prefs,
    const std::vector<raw_ptr<const PrepopulatedEngine>>&
        regional_prepopulated_engines) {
  // Ghosium privacy baseline: never use a Google-owned fallback provider.
  // User-selected providers still keep normal Chromium precedence elsewhere.
  return PrepopulatedEngineToTemplateURLData(&duckduckgo);
}
'@
  $updated = [regex]::Replace($text, $functionPattern, $replacement, 1)
  [IO.File]::WriteAllText($target, $updated, [Text.UTF8Encoding]::new($false))
  $text = $updated
} elseif (!$alreadyHardened) {
  throw 'Pinned Chromium fallback-search behavior changed; expected the reviewed Google fallback or exact Ghosium DuckDuckGo fallback.'
}

$finalMatch = [regex]::Match($text, $functionPattern)
if (!$finalMatch.Success) {
  throw 'Ghosium fallback-search function disappeared after rewrite.'
}
$finalBody = [regex]::Replace($finalMatch.Groups['body'].Value, '\s+', ' ').Trim()
if (!$finalBody.Contains('PrepopulatedEngineToTemplateURLData(&duckduckgo);')) {
  throw 'DuckDuckGo is not the Ghosium fallback search provider.'
}
foreach ($forbidden in @('google.id', 'use_first_as_fallback')) {
  if ($finalBody.Contains($forbidden)) {
    throw "Google-owned fallback behavior remains active in Ghosium: $forbidden"
  }
}

$thirdPartyChanges = & git -C $sourceRootResolved status --porcelain=v1 -- third_party
if ($LASTEXITCODE -ne 0) {
  throw 'Unable to verify third_party source state after default-search rewrite.'
}
if ($thirdPartyChanges) {
  throw 'Default-search rewrite modified third_party sources; refusing to continue.'
}

Write-Host 'Ghosium fallback search hardened: DuckDuckGo built-in fallback active; Google fallback disabled.'
