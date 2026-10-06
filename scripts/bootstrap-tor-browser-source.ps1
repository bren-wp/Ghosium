param(
  [Parameter(Mandatory = $true)]
  [string]$Destination,

  [Parameter(Mandatory = $false)]
  [string]$CacheDir
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$contractPath = Join-Path $repoRoot 'engine/tor-browser/windows-x64.json'

& (Join-Path $PSScriptRoot 'verify-tor-browser-upstream.ps1')
if ($LASTEXITCODE -ne 0) {
  throw 'Tor Browser upstream contract verification failed.'
}

$contract = Get-Content $contractPath -Raw | ConvertFrom-Json
$destinationPath = [IO.Path]::GetFullPath($Destination)

if ([string]::IsNullOrWhiteSpace($CacheDir)) {
  $CacheDir = Join-Path ([IO.Path]::GetTempPath()) 'ghosium-tor-browser-cache'
}
$cachePath = [IO.Path]::GetFullPath($CacheDir)
New-Item -ItemType Directory -Force -Path $cachePath | Out-Null

$archivePath = Join-Path $cachePath ([string]$contract.sourceArchive)
$expectedHash = ([string]$contract.sha256).ToLowerInvariant()

function Test-PinnedArchive {
  param([Parameter(Mandatory = $true)][string]$Path)
  if (!(Test-Path $Path -PathType Leaf) -or (Get-Item $Path).Length -le 0) {
    return $false
  }
  return (Get-FileHash $Path -Algorithm SHA256).Hash.ToLowerInvariant() -eq $expectedHash
}

if (!(Test-PinnedArchive -Path $archivePath)) {
  Remove-Item $archivePath -Force -ErrorAction SilentlyContinue
  Write-Host "Downloading Tor Browser source $($contract.torBrowserVersion)..."
  Invoke-WebRequest -Uri ([string]$contract.sourceUrl) -OutFile $archivePath -MaximumRedirection 5

  if (!(Test-PinnedArchive -Path $archivePath)) {
    Remove-Item $archivePath -Force -ErrorAction SilentlyContinue
    throw 'Downloaded Tor Browser source failed the pinned SHA-256 contract.'
  }
}

if (Test-Path $destinationPath) {
  $existing = @(Get-ChildItem $destinationPath -Force -ErrorAction Stop)
  if ($existing.Count -gt 0) {
    throw "Tor Browser source destination must be empty: $destinationPath"
  }
} else {
  New-Item -ItemType Directory -Force -Path $destinationPath | Out-Null
}

$tar = Get-Command tar -ErrorAction Stop
& $tar.Source -xf $archivePath -C $destinationPath
if ($LASTEXITCODE -ne 0) {
  throw "Tor Browser source extraction failed with exit code $LASTEXITCODE"
}

$reparse = Get-ChildItem $destinationPath -Recurse -Force |
  Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint } |
  Select-Object -First 1
if ($reparse) {
  throw "Tor Browser source archive materialized an unexpected reparse point: $($reparse.FullName)"
}

$roots = @(Get-ChildItem $destinationPath -Directory -Force)
if ($roots.Count -ne 1) {
  throw "Expected exactly one extracted Tor Browser source root; found $($roots.Count)."
}
$sourceRoot = $roots[0].FullName

foreach ($required in @('browser','toolkit','dom','netwerk','security')) {
  if (!(Test-Path (Join-Path $sourceRoot $required) -PathType Container)) {
    throw "Pinned Tor Browser source is missing expected Firefox/Tor Browser directory: $required"
  }
}

$mach = Join-Path $sourceRoot 'mach'
if (!(Test-Path $mach -PathType Leaf)) {
  throw 'Pinned Tor Browser source is missing the Mozilla mach build entrypoint.'
}

$evidence = [ordered]@{
  schemaVersion = 1
  product = 'Ghosium Browser'
  role = 'Tor Browser/Firefox ESR engine source bootstrap'
  torBrowserVersion = [string]$contract.torBrowserVersion
  firefoxVersion = [string]$contract.firefoxVersion
  torVersion = [string]$contract.torVersion
  archive = [string]$contract.sourceArchive
  archiveSha256 = $expectedHash
  sourceRoot = $sourceRoot
  chromiumFallbackAllowed = $false
}

$evidencePath = Join-Path $destinationPath 'GHOSIUM-TOR-BROWSER-SOURCE.json'
[IO.File]::WriteAllText(
  $evidencePath,
  (($evidence | ConvertTo-Json -Depth 5) + [Environment]::NewLine),
  [Text.UTF8Encoding]::new($false)
)

Write-Host "Pinned Tor Browser source ready: $sourceRoot"
Write-Host "Evidence: $evidencePath"
