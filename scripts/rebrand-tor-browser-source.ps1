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

Write-Host "Ghosium Tor Browser source branding applied to $changed brand resource file(s)."
