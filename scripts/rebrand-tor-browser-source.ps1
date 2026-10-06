param(
  [Parameter(Mandatory = $true)]
  [string]$SourceRoot
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$source = (Resolve-Path $SourceRoot).Path
$brandRoot = Join-Path $source 'browser/branding/tb-release'
if (!(Test-Path $brandRoot -PathType Container)) {
  throw "Pinned Tor Browser source is missing browser/branding/tb-release: $brandRoot"
}

$brandFiles = @(
  Get-ChildItem $brandRoot -Recurse -File |
    Where-Object { $_.Name -in @('brand.ftl','brand.properties') }
)
if ($brandFiles.Count -lt 1) {
  throw 'Tor Browser tb-release branding files were not found.'
}

$changed = 0
foreach ($file in $brandFiles) {
  $text = Get-Content $file.FullName -Raw
  $updated = $text.Replace('Tor Browser', 'Ghosium Browser').Replace('TorBrowser', 'Ghosium')
  if ($updated -ne $text) {
    [IO.File]::WriteAllText($file.FullName, $updated, [Text.UTF8Encoding]::new($false))
    $changed++
  }
}

if ($changed -lt 1) {
  throw 'Tor Browser product-name branding transform changed no files.'
}

$mark = Join-Path $repoRoot 'engine/branding/ghosium-mark.svg'
$wordmark = Join-Path $brandRoot 'content/about-wordmark.svg'
if (!(Test-Path $mark -PathType Leaf)) {
  throw 'Canonical Ghosium SVG mark is missing.'
}
if (!(Test-Path $wordmark -PathType Leaf)) {
  throw 'Pinned Tor Browser release branding is missing content/about-wordmark.svg.'
}
Copy-Item $mark $wordmark -Force

foreach ($file in $brandFiles) {
  $text = Get-Content $file.FullName -Raw
  if ($text.Contains('Tor Browser') -or $text.Contains('TorBrowser')) {
    throw "Tor Browser product branding remains in $($file.FullName)"
  }
  if (!$text.Contains('Ghosium')) {
    throw "Ghosium branding was not materialized in $($file.FullName)"
  }
}

$wordmarkText = Get-Content $wordmark -Raw
if (!$wordmarkText.Contains('<svg')) {
  throw 'Ghosium about wordmark is not valid SVG text.'
}


# Preserve the existing Ghosium visual identity on Firefox/Tor Browser's built-in New Tab.
$newTabRoot = Join-Path $source 'browser/extensions/newtab'
$newTabHtmlTarget = Join-Path $newTabRoot 'prerendered/activity-stream.html'
$newTabNoScriptTarget = Join-Path $newTabRoot 'prerendered/activity-stream-noscripts.html'
$newTabDebugTarget = Join-Path $newTabRoot 'prerendered/activity-stream-debug.html'
$newTabCssTarget = Join-Path $newTabRoot 'css/activity-stream.css'
$newTabAssets = Join-Path $newTabRoot 'data/content/assets'

foreach ($required in @($newTabRoot, (Split-Path $newTabHtmlTarget -Parent), (Split-Path $newTabCssTarget -Parent), $newTabAssets)) {
  if (!(Test-Path $required -PathType Container)) {
    throw "Pinned Tor Browser source is missing expected New Tab path: $required"
  }
}

$uiHtmlSource = Join-Path $repoRoot 'extension/newtab.html'
$uiCssSource = Join-Path $repoRoot 'extension/newtab.css'
$uiMarkSource = Join-Path $repoRoot 'extension/ghosium-mark.svg'
foreach ($required in @($uiHtmlSource, $uiCssSource, $uiMarkSource)) {
  if (!(Test-Path $required -PathType Leaf)) {
    throw "Canonical Ghosium UI asset is missing: $required"
  }
}

$uiHtml = Get-Content $uiHtmlSource -Raw
$uiHtml = $uiHtml.Replace('<script src="newtab.js" defer></script>', '')
$uiHtml = $uiHtml.Replace('href="newtab.css"', 'href="chrome://newtab/content/css/activity-stream.css"')
$uiHtml = $uiHtml.Replace('src="ghosium-mark.svg"', 'src="chrome://newtab/content/data/content/assets/ghosium-mark.svg"')
$uiHtml = $uiHtml.Replace('class="local-option" href="options.html"', 'class="local-option" href="about:preferences"')
$uiHtml = $uiHtml.Replace(
  '<meta charset="utf-8">',
  '<meta charset="utf-8">' + [Environment]::NewLine +
  '  <meta http-equiv="Content-Security-Policy" content="default-src ''none''; style-src chrome:; img-src chrome: data:; form-action https://duckduckgo.com;">'
)

foreach ($target in @($newTabHtmlTarget, $newTabNoScriptTarget)) {
  [IO.File]::WriteAllText($target, $uiHtml, [Text.UTF8Encoding]::new($false))
}
if (Test-Path $newTabDebugTarget -PathType Leaf) {
  [IO.File]::WriteAllText($newTabDebugTarget, $uiHtml, [Text.UTF8Encoding]::new($false))
}
Copy-Item $uiCssSource $newTabCssTarget -Force
Copy-Item $uiMarkSource (Join-Path $newTabAssets 'ghosium-mark.svg') -Force

# Preserve the same Ghosium logo across Tor Browser release-branding icon surfaces when matching targets exist.
$pngIconMap = @{
  'default16.png' = (Join-Path $repoRoot 'extension/icons/16.png')
  'default32.png' = (Join-Path $repoRoot 'extension/icons/32.png')
  'default48.png' = (Join-Path $repoRoot 'extension/icons/48.png')
  'default128.png' = (Join-Path $repoRoot 'extension/icons/128.png')
}
foreach ($entry in $pngIconMap.GetEnumerator()) {
  $targets = @(Get-ChildItem $brandRoot -Recurse -File -Filter $entry.Key -ErrorAction SilentlyContinue)
  if ((Test-Path $entry.Value -PathType Leaf) -and $targets.Count -gt 0) {
    foreach ($target in $targets) {
      Copy-Item $entry.Value $target.FullName -Force
    }
  }
}
$icoSource = Join-Path $repoRoot 'ghosium.ico'
if (Test-Path $icoSource -PathType Leaf) {
  foreach ($target in @(Get-ChildItem $brandRoot -Recurse -File -Filter '*.ico' -ErrorAction SilentlyContinue)) {
    Copy-Item $icoSource $target.FullName -Force
  }
}

$renderedHtml = Get-Content $newTabHtmlTarget -Raw
$renderedCss = Get-Content $newTabCssTarget -Raw
foreach ($token in @('GHOSIUM BROWSER', 'Browse freely.', 'Stay private.', 'duckduckgo.com', 'ghosium-mark.svg')) {
  if (!$renderedHtml.Contains($token)) {
    throw "Ghosium New Tab source port is missing required UI token: $token"
  }
}
foreach ($token in @('--bg: #111016', '--aurora: #8af0c7', '.search', '.status')) {
  if (!$renderedCss.Contains($token)) {
    throw "Ghosium New Tab CSS port is missing required design token: $token"
  }
}

Write-Host "Ghosium Tor Browser source branding applied to $changed brand resource file(s)."
