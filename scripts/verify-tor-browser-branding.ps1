param(
  [Parameter(Mandatory = $true)]
  [string]$SourceRoot
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$source = (Resolve-Path $SourceRoot).Path
$brandRoot = Join-Path $source 'browser/branding/tb-release'
if (!(Test-Path $brandRoot -PathType Container)) {
  throw 'Ghosium Tor Browser branding root is missing.'
}

$brandFiles = @(Get-ChildItem $brandRoot -Recurse -File | Where-Object { $_.Name -in @('brand.ftl','brand.properties') })
if ($brandFiles.Count -lt 1) {
  throw 'No Tor Browser brand.ftl/brand.properties files were available for verification.'
}
foreach ($file in $brandFiles) {
  $text = Get-Content $file.FullName -Raw
  if ($text.Contains('Tor Browser') -or $text.Contains('TorBrowser')) {
    throw "Upstream Tor Browser product name remains in active brand resource: $($file.FullName)"
  }
  if (!$text.Contains('Ghosium')) {
    throw "Active brand resource does not contain Ghosium identity: $($file.FullName)"
  }
}

$wordmark = Join-Path $brandRoot 'content/about-wordmark.svg'
if (!(Test-Path $wordmark -PathType Leaf)) {
  throw 'Ghosium about wordmark is missing.'
}
if (!(Get-Content $wordmark -Raw).Contains('<svg')) {
  throw 'Ghosium about wordmark failed SVG verification.'
}


$newTabRoot = Join-Path $source 'browser/extensions/newtab'
$newTabHtml = Join-Path $newTabRoot 'prerendered/activity-stream.html'
$newTabCss = Join-Path $newTabRoot 'css/activity-stream.css'
$newTabMark = Join-Path $newTabRoot 'data/content/assets/ghosium-mark.svg'
foreach ($required in @($newTabHtml, $newTabCss, $newTabMark)) {
  if (!(Test-Path $required -PathType Leaf)) {
    throw "Ghosium New Tab source asset is missing: $required"
  }
}

$html = Get-Content $newTabHtml -Raw
$css = Get-Content $newTabCss -Raw
foreach ($token in @('GHOSIUM BROWSER', 'Browse freely.', 'Stay private.', 'duckduckgo.com', 'chrome://newtab/content/css/activity-stream.css')) {
  if (!$html.Contains($token)) {
    throw "Ghosium New Tab verification failed; missing: $token"
  }
}
foreach ($token in @('--bg: #111016', '--aurora: #8af0c7', '--neon: #62e7d5', '.product-nav', '.search', '.status')) {
  if (!$css.Contains($token)) {
    throw "Ghosium New Tab CSS verification failed; missing: $token"
  }
}
if ($html.Contains('newtab.js') -or $html.Contains('options.html')) {
  throw 'Ghosium New Tab still references Chromium-extension-only runtime surfaces.'
}

Write-Host 'Ghosium Tor Browser source branding: OK'
