param(
  [Parameter(Mandatory = $false)]
  [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
  $OutputPath = Join-Path $repoRoot 'ghosium.ico'
}
$outputFull = [IO.Path]::GetFullPath($OutputPath)
$outputDir = Split-Path -Parent $outputFull
if ($outputDir -and !(Test-Path $outputDir -PathType Container)) {
  New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}

$frames = @()
foreach ($size in @(16, 32, 48, 128)) {
  $path = Join-Path $repoRoot "extension/icons/$size.png"
  if (!(Test-Path $path -PathType Leaf)) {
    throw "Canonical Ghosium PNG icon frame is missing: $path"
  }
  $bytes = [IO.File]::ReadAllBytes($path)
  $png = [byte[]](0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A)
  if ($bytes.Length -lt 24) {
    throw "Canonical Ghosium PNG icon frame is too small: $path"
  }
  for ($i = 0; $i -lt $png.Length; $i++) {
    if ($bytes[$i] -ne $png[$i]) {
      throw "Canonical Ghosium icon frame is not PNG: $path"
    }
  }
  $frames += [pscustomobject]@{ Size = $size; Bytes = $bytes; Path = $path }
}

$temp = "$outputFull.tmp-$PID"
$stream = $null
$writer = $null
try {
  $stream = [IO.File]::Open($temp, [IO.FileMode]::Create, [IO.FileAccess]::Write, [IO.FileShare]::None)
  $writer = [IO.BinaryWriter]::new($stream)

  $writer.Write([UInt16]0)
  $writer.Write([UInt16]1)
  $writer.Write([UInt16]$frames.Count)

  [UInt32]$offset = 6 + (16 * $frames.Count)
  foreach ($frame in $frames) {
    $dimension = if ($frame.Size -ge 256) { [byte]0 } else { [byte]$frame.Size }
    $writer.Write($dimension)
    $writer.Write($dimension)
    $writer.Write([byte]0)
    $writer.Write([byte]0)
    $writer.Write([UInt16]1)
    $writer.Write([UInt16]32)
    $writer.Write([UInt32]$frame.Bytes.Length)
    $writer.Write([UInt32]$offset)
    $offset += [UInt32]$frame.Bytes.Length
  }

  foreach ($frame in $frames) {
    $writer.Write([byte[]]$frame.Bytes)
  }
  $writer.Flush()
} finally {
  if ($writer) { $writer.Dispose() }
  elseif ($stream) { $stream.Dispose() }
}

[IO.File]::Move($temp, $outputFull, $true)

$icon = [IO.File]::ReadAllBytes($outputFull)
if ($icon.Length -lt 64 -or
    $icon[0] -ne 0 -or $icon[1] -ne 0 -or
    $icon[2] -ne 1 -or $icon[3] -ne 0) {
  throw 'Generated Ghosium ICO header is invalid.'
}
$count = [BitConverter]::ToUInt16($icon, 4)
if ($count -ne 4) {
  throw "Generated Ghosium ICO frame count is invalid: $count"
}

$sha = (Get-FileHash $outputFull -Algorithm SHA256).Hash.ToLowerInvariant()
Write-Host "Canonical Ghosium Windows icon generated from existing logo frames: $outputFull / sha256=$sha"
Write-Output $outputFull
