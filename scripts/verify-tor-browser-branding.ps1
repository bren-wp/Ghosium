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

Write-Host 'Ghosium Tor Browser source branding: OK'
