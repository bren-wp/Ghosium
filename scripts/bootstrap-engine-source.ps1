param(
  [Parameter(Mandatory = $false)]
  [string]$Destination = 'engine-work',

  [Parameter(Mandatory = $false)]
  [switch]$SkipHooks,

  [Parameter(Mandatory = $false)]
  [switch]$ReuseExisting
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Invoke-GhosiumBoundedProcess {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Command,

    [Parameter(Mandatory = $true)]
    [string[]]$Arguments,

    [Parameter(Mandatory = $true)]
    [string]$WorkingDirectory,

    [Parameter(Mandatory = $true)]
    [ValidateRange(30, 7200)]
    [int]$TimeoutSeconds,

    [Parameter(Mandatory = $true)]
    [string]$Description
  )

  $stdoutPath = Join-Path $env:RUNNER_TEMP ("ghosium-" + [guid]::NewGuid().ToString('N') + ".stdout.log")
  $stderrPath = Join-Path $env:RUNNER_TEMP ("ghosium-" + [guid]::NewGuid().ToString('N') + ".stderr.log")
  Write-Host "$Description (timeout: $TimeoutSeconds seconds)"
  $process = Start-Process -FilePath $Command -ArgumentList $Arguments -WorkingDirectory $WorkingDirectory -NoNewWindow -PassThru -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath
  $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)

  try {
    while (!$process.HasExited) {
      if ([DateTime]::UtcNow -ge $deadline) {
        Write-Error "$Description exceeded its $TimeoutSeconds second timeout. Terminating process tree PID $($process.Id)."
        & "$env:SystemRoot\System32\taskkill.exe" /PID $process.Id /T /F | Out-Host
        $process.WaitForExit()
        throw "$Description timed out after $TimeoutSeconds seconds."
      }

      Start-Sleep -Seconds 30
      $process.Refresh()
      if (!$process.HasExited) {
        Write-Host "$Description still running (PID $($process.Id))..."
      }
    }

    if (Test-Path $stdoutPath -PathType Leaf) {
      Get-Content $stdoutPath | Out-Host
    }
    if (Test-Path $stderrPath -PathType Leaf) {
      Get-Content $stderrPath | ForEach-Object { Write-Host $_ }
    }

    if ($process.ExitCode -ne 0) {
      throw "$Description failed with exit code $($process.ExitCode)."
    }
  } finally {
    Remove-Item $stdoutPath,$stderrPath -Force -ErrorAction SilentlyContinue
    if (!$process.HasExited) {
      & "$env:SystemRoot\System32\taskkill.exe" /PID $process.Id /T /F 2>$null | Out-Null
    }
    $process.Dispose()
  }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$sourceRevision = (Get-Content (Join-Path $repoRoot 'ENGINE_SOURCE_REVISION') -Raw).Trim()
if ($sourceRevision -notmatch '^[0-9a-f]{40}$') {
  throw 'ENGINE_SOURCE_REVISION is not a valid pinned Git commit.'
}

$fetchCommand = Get-Command fetch -ErrorAction SilentlyContinue
$gclientCommand = Get-Command gclient -ErrorAction SilentlyContinue
if (!$fetchCommand -or !$gclientCommand) {
  throw 'Chromium depot_tools must be installed and on PATH before bootstrapping the full-source checkout.'
}

if ([System.Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT) {
  $env:DEPOT_TOOLS_WIN_TOOLCHAIN = '0'

  # depot_tools git_cache.py intentionally invokes git.bat on Windows. The
  # GitHub-hosted Windows image exposes Git as git.exe but does not provide a
  # compatible git.bat command. Keep the pinned depot_tools checkout immutable
  # and provide an isolated hosted-only forwarding shim to the exact resolved
  # Git executable instead of modifying Chromium tooling.
  if ($env:GITHUB_ACTIONS -eq 'true' -and !(Get-Command git.bat -ErrorAction SilentlyContinue)) {
    $gitCommand = Get-Command git.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if (!$gitCommand -or [string]::IsNullOrWhiteSpace([string]$gitCommand.Source)) {
      throw 'GitHub-hosted Chromium bootstrap requires a resolvable git.exe before creating the depot_tools git.bat compatibility shim.'
    }

    $shimRoot = Join-Path $env:RUNNER_TEMP 'ghosium-git-shim'
    New-Item -ItemType Directory -Force -Path $shimRoot | Out-Null
    $shimPath = Join-Path $shimRoot 'git.bat'
    $shimLines = @(
      '@echo off'
      "`"$([IO.Path]::GetFullPath([string]$gitCommand.Source))`" %*"
    )
    [IO.File]::WriteAllText(
      $shimPath,
      (($shimLines -join "`r`n") + "`r`n"),
      [Text.Encoding]::ASCII
    )
    $env:PATH = "$shimRoot;$($env:PATH)"

    $resolvedShim = Get-Command git.bat -ErrorAction SilentlyContinue | Select-Object -First 1
    if (!$resolvedShim -or [IO.Path]::GetFullPath([string]$resolvedShim.Source) -ne [IO.Path]::GetFullPath($shimPath)) {
      throw 'Unable to expose the isolated hosted git.bat compatibility shim to depot_tools.'
    }
    Write-Host "GitHub-hosted depot_tools Git compatibility shim: $shimPath -> $($gitCommand.Source)"
  }
}

# The full-source workflow intentionally supplies an absolute persistent Windows
# workspace. Join-Path does not treat an absolute ChildPath as replacing its
# parent (for example, Join-Path C:\repo C:\src yields an invalid composite
# path), so absolute and relative destinations must be resolved separately.
if ([IO.Path]::IsPathFullyQualified($Destination)) {
  $destinationPath = [IO.Path]::GetFullPath($Destination)
} else {
  $destinationPath = [IO.Path]::GetFullPath((Join-Path (Get-Location).Path $Destination))
}
$src = Join-Path $destinationPath 'src'
$existingItems = @()
if (Test-Path $destinationPath) {
  $existingItems = @(Get-ChildItem $destinationPath -Force -ErrorAction SilentlyContinue)
}

$reuseCheckout = $existingItems.Count -gt 0
if ($reuseCheckout -and !$ReuseExisting) {
  throw "Destination is not empty. Pass -ReuseExisting only for a controlled Chromium builder checkout: $destinationPath"
}

if ($reuseCheckout) {
  if (!(Test-Path (Join-Path $src '.git')) -or !(Test-Path (Join-Path $destinationPath '.gclient') -PathType Leaf)) {
    throw "Existing destination is not a reusable Chromium depot_tools checkout: $destinationPath"
  }

  Write-Host "Reusing controlled Chromium checkout at $destinationPath"
  & git -C $src reset --hard HEAD
  if ($LASTEXITCODE -ne 0) {
    throw 'Unable to reset the reusable Chromium checkout.'
  }
  & git -C $src clean -ffd
  if ($LASTEXITCODE -ne 0) {
    throw 'Unable to remove untracked source files from the reusable Chromium checkout.'
  }
} else {
  New-Item -ItemType Directory -Path $destinationPath -Force | Out-Null
  Push-Location $destinationPath
  try {
    Write-Host "Fetching Chromium source for Ghosium into $destinationPath"
    Invoke-GhosiumBoundedProcess `
      -Command $fetchCommand.Source `
      -Arguments @('--nohooks', '--no-history', 'chromium') `
      -WorkingDirectory $destinationPath `
      -TimeoutSeconds 1800 `
      -Description 'Initial Chromium source fetch'
  } finally {
    Pop-Location
  }

  if (!(Test-Path (Join-Path $src '.git'))) {
    throw 'Chromium fetch did not produce the expected src Git checkout.'
  }
}

$fetchSucceeded = $false
$fetchAttempts = 3
for ($attempt = 1; $attempt -le $fetchAttempts; $attempt++) {
  try {
    Invoke-GhosiumBoundedProcess `
      -Command (Get-Command git.exe -ErrorAction Stop).Source `
      -Arguments @('-C', $src, 'fetch', 'origin', $sourceRevision, '--no-tags') `
      -WorkingDirectory $destinationPath `
      -TimeoutSeconds 900 `
      -Description "Pinned Chromium revision fetch attempt $attempt/$fetchAttempts"
    $fetchSucceeded = $true
    break
  } catch {
    if ($attempt -ge $fetchAttempts) {
      throw
    }
    $retryDelaySeconds = 15 * $attempt
    Write-Warning "Pinned Chromium fetch attempt $attempt/$fetchAttempts failed: $($_.Exception.Message). Retrying in $retryDelaySeconds seconds."
    Start-Sleep -Seconds $retryDelaySeconds
  }
}
if (!$fetchSucceeded) {
  throw "Unable to fetch pinned Chromium commit $sourceRevision after $fetchAttempts attempts"
}
& git -C $src checkout --detach $sourceRevision
if ($LASTEXITCODE -ne 0) {
  throw "Unable to detach Chromium checkout at $sourceRevision"
}
& git -C $src reset --hard $sourceRevision
if ($LASTEXITCODE -ne 0) {
  throw "Unable to reset Chromium checkout to pinned revision $sourceRevision"
}

Push-Location $destinationPath
try {
  # A persistent self-hosted checkout contains many gclient-managed Git
  # dependencies that are not cleaned by `git reset/clean` in src alone. Force
  # every dependency back to the exact DEPS state before hooks or compilation so
  # stale local edits and removed dependency trees cannot influence a later
  # verified Ghosium build.
  $syncArguments = @(
    'sync',
    '--reset',
    '--delete_unversioned_trees',
    '--force',
    '--with_branch_heads',
    '--with_tags',
    '--revision',
    "src@$sourceRevision"
  )
  if ($SkipHooks) {
    $syncArguments += '--nohooks'
  }
  Invoke-GhosiumBoundedProcess `
    -Command $gclientCommand.Source `
    -Arguments $syncArguments `
    -WorkingDirectory $destinationPath `
    -TimeoutSeconds 1800 `
    -Description 'Pinned Chromium gclient sync'

  if (!$SkipHooks) {
    Invoke-GhosiumBoundedProcess `
      -Command $gclientCommand.Source `
      -Arguments @('runhooks') `
      -WorkingDirectory $destinationPath `
      -TimeoutSeconds 1200 `
      -Description 'Pinned Chromium gclient runhooks'
  }
} finally {
  Pop-Location
}

$actualRevision = (& git -C $src rev-parse HEAD).Trim()
if ($actualRevision -ne $sourceRevision) {
  throw "Checkout drifted from pinned revision. Expected $sourceRevision; found $actualRevision"
}

Write-Host "Pinned Chromium source ready at $src"
Write-Host "Next: $PSScriptRoot/apply-engine-branding.ps1 -SourceRoot '$src'"