param(
  [Parameter(Mandatory = $true)]
  [string]$StageDir,

  [Parameter(Mandatory = $false)]
  [string]$CacheDir,

  [Parameter(Mandatory = $false)]
  [string]$ReportPath
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if ([System.Environment]::OSVersion.Platform -ne [System.PlatformID]::Win32NT) {
  throw 'Ghosium Tor runtime staging is currently supported only on Windows.'
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$contractPath = Join-Path $repoRoot 'engine/tor/windows-x64.json'
if (!(Test-Path $contractPath -PathType Leaf)) {
  throw 'Pinned Ghosium Tor runtime contract is missing.'
}
$contract = Get-Content $contractPath -Raw | ConvertFrom-Json
if ([int]$contract.schemaVersion -ne 1 -or
    [string]$contract.platform -ne 'windows-x86_64' -or
    [string]$contract.sha256 -notmatch '^[0-9a-f]{64}$') {
  throw 'Pinned Ghosium Tor runtime contract is invalid.'
}

$stagePath = [IO.Path]::GetFullPath($StageDir)
if (!(Test-Path $stagePath -PathType Container)) {
  throw "Ghosium release stage does not exist: $stagePath"
}

if ([string]::IsNullOrWhiteSpace($CacheDir)) {
  $CacheDir = Join-Path $env:RUNNER_TEMP 'ghosium-tor-cache'
}
$cachePath = [IO.Path]::GetFullPath($CacheDir)
New-Item -ItemType Directory -Force -Path $cachePath | Out-Null

$archivePath = Join-Path $cachePath ([string]$contract.archive)
$expectedHash = ([string]$contract.sha256).ToLowerInvariant()

function Assert-ArchiveHash {
  param([Parameter(Mandatory = $true)][string]$Path)
  if (!(Test-Path $Path -PathType Leaf) -or (Get-Item $Path).Length -le 0) {
    return $false
  }
  $actual = (Get-FileHash $Path -Algorithm SHA256).Hash.ToLowerInvariant()
  return $actual -eq $expectedHash
}

if (!(Assert-ArchiveHash -Path $archivePath)) {
  Remove-Item $archivePath -Force -ErrorAction SilentlyContinue
  Write-Host "Downloading pinned Tor Expert Bundle $($contract.version)..."
  Invoke-WebRequest -Uri ([string]$contract.url) -OutFile $archivePath -MaximumRedirection 5
  if (!(Assert-ArchiveHash -Path $archivePath)) {
    Remove-Item $archivePath -Force -ErrorAction SilentlyContinue
    throw 'Downloaded Tor Expert Bundle failed the pinned SHA-256 contract.'
  }
}

$extractRoot = Join-Path $env:RUNNER_TEMP ('ghosium-tor-extract-' + [Guid]::NewGuid().ToString('N'))
$runtimePath = Join-Path $stagePath ([string]$contract.normalizedRuntimeDirectory)
New-Item -ItemType Directory -Force -Path $extractRoot | Out-Null
try {
  $tar = Get-Command tar.exe -ErrorAction Stop
  & $tar.Source -xzf $archivePath -C $extractRoot
  if ($LASTEXITCODE -ne 0) {
    throw "Tor Expert Bundle extraction failed with exit code $LASTEXITCODE"
  }

  $reparse = Get-ChildItem $extractRoot -Recurse -Force |
    Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint } |
    Select-Object -First 1
  if ($reparse) {
    throw "Tor Expert Bundle contains a reparse point; refusing packaging: $($reparse.FullName)"
  }

  $torExecutables = @(Get-ChildItem $extractRoot -File -Recurse -Filter 'tor.exe' -ErrorAction Stop)
  if ($torExecutables.Count -ne 1) {
    throw "Tor Expert Bundle must contain exactly one tor.exe; found $($torExecutables.Count)."
  }

  $torSourceDir = $torExecutables[0].Directory.FullName
  if (Test-Path $runtimePath) {
    Remove-Item $runtimePath -Recurse -Force
  }
  New-Item -ItemType Directory -Force -Path $runtimePath | Out-Null
  Get-ChildItem $torSourceDir -Force | ForEach-Object {
    Copy-Item $_.FullName -Destination $runtimePath -Recurse -Force
  }

  $stagedTor = Join-Path $runtimePath 'tor.exe'
  if (!(Test-Path $stagedTor -PathType Leaf) -or (Get-Item $stagedTor).Length -le 0) {
    throw 'Normalized Ghosium Tor runtime is missing Tor/tor.exe.'
  }

  $lyrebird = Get-ChildItem $runtimePath -File -Recurse -Filter 'lyrebird.exe' -ErrorAction SilentlyContinue |
    Select-Object -First 1

  $files = @(Get-ChildItem $runtimePath -File -Recurse -Force)
  if ($files.Count -lt 2) {
    throw 'Normalized Ghosium Tor runtime is unexpectedly incomplete.'
  }

  $report = [ordered]@{
    schemaVersion = 1
    component = [string]$contract.component
    publisher = [string]$contract.publisher
    version = [string]$contract.version
    platform = [string]$contract.platform
    sourceUrl = [string]$contract.url
    archive = [string]$contract.archive
    archiveSha256 = $expectedHash
    normalizedExecutable = 'Tor/tor.exe'
    torExecutableSha256 = (Get-FileHash $stagedTor -Algorithm SHA256).Hash.ToLowerInvariant()
    pluggableTransportIncluded = [bool]$lyrebird
    fileCount = $files.Count
    totalBytes = [int64](($files | Measure-Object Length -Sum).Sum)
  }

  $embeddedReport = Join-Path $runtimePath 'GHOSIUM-TOR-RUNTIME.json'
  [IO.File]::WriteAllText(
    $embeddedReport,
    (($report | ConvertTo-Json -Depth 5) + [Environment]::NewLine),
    [Text.UTF8Encoding]::new($false)
  )

  if (![string]::IsNullOrWhiteSpace($ReportPath)) {
    $reportFull = [IO.Path]::GetFullPath($ReportPath)
    $reportDir = Split-Path -Parent $reportFull
    if ($reportDir -and !(Test-Path $reportDir -PathType Container)) {
      New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
    }
    Copy-Item $embeddedReport $reportFull -Force
  }

  Write-Host "Pinned Tor runtime staged: $stagedTor"
  Write-Host "Tor Expert Bundle SHA-256: $expectedHash"
} finally {
  Remove-Item $extractRoot -Recurse -Force -ErrorAction SilentlyContinue
}
