param(
  [Parameter(Mandatory = $true)]
  [string]$Version,

  [Parameter(Mandatory = $false)]
  [string]$OutputPath = 'staging/Ghosium-Browser.exe'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if ($Version -notmatch '^(\d+)\.(\d+)\.(\d+)$') {
  throw "Version must be semantic x.y.z; received '$Version'"
}

$major = $Matches[1]
$minor = $Matches[2]
$patch = $Matches[3]

$iconGenerator = 'scripts/ensure-ghosium-icon.ps1'
if (!(Test-Path $iconGenerator -PathType Leaf)) {
  throw "Canonical Ghosium icon generator is missing: $iconGenerator"
}
& $iconGenerator -OutputPath ([IO.Path]::GetFullPath('ghosium.ico')) | Out-Host

$required = @(
  'launcher/main.cpp',
  'launcher/ghosium.rc',
  'ghosium.ico'
)
foreach ($path in $required) {
  if (!(Test-Path $path -PathType Leaf)) {
    throw "Required native launcher input is missing: $path"
  }
}

$iconPath = [IO.Path]::GetFullPath('ghosium.ico')
if (!(Test-Path $iconPath -PathType Leaf)) {
  throw 'Canonical Ghosium Windows icon is missing.'
}
$iconBytes = [IO.File]::ReadAllBytes($iconPath)
if ($iconBytes.Length -lt 22 -or $iconBytes[0] -ne 0 -or $iconBytes[1] -ne 0 -or $iconBytes[2] -ne 1 -or $iconBytes[3] -ne 0) {
  throw 'Canonical ghosium.ico has an invalid ICO header.'
}
$iconCount = [BitConverter]::ToUInt16($iconBytes, 4)
if ($iconCount -lt 4) { throw "Canonical ghosium.ico has too few image frames: $iconCount" }

$vswhere = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
if (!(Test-Path $vswhere -PathType Leaf)) { throw 'vswhere.exe was not found.' }
$vsPath = (& $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath | Select-Object -First 1)
if (!$vsPath) { throw 'Visual Studio C++ toolchain was not found.' }
$devCmd = Join-Path $vsPath 'Common7\Tools\VsDevCmd.bat'
if (!(Test-Path $devCmd -PathType Leaf)) { throw "Visual Studio developer environment script was not found: $devCmd" }

$outputFullPath = [IO.Path]::GetFullPath($OutputPath)
$outputDir = Split-Path -Parent $outputFullPath
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
$resPath = Join-Path $outputDir 'ghosium-launcher.res'

$commands = @(
  "call `"$devCmd`" -arch=x64 -host_arch=x64",
  "rc.exe /nologo /dGHOSIUM_VERSION_MAJOR=$major /dGHOSIUM_VERSION_MINOR=$minor /dGHOSIUM_VERSION_PATCH=$patch /fo `"$resPath`" launcher\ghosium.rc",
  "cl.exe /nologo /std:c++20 /O2 /W4 /EHsc /DUNICODE /D_UNICODE /GS /sdl /guard:cf /MT launcher\main.cpp `"$resPath`" /Fe:`"$outputFullPath`" /link /SUBSYSTEM:WINDOWS /DYNAMICBASE /NXCOMPAT /HIGHENTROPYVA /GUARD:CF /CETCOMPAT user32.lib shell32.lib ws2_32.lib"
)
& cmd.exe /d /s /c ($commands -join ' && ')
if ($LASTEXITCODE -ne 0) { throw "Ghosium C++20 launcher/resource build failed with exit code $LASTEXITCODE" }
if (!(Test-Path $outputFullPath -PathType Leaf)) { throw "Ghosium launcher was not produced: $outputFullPath" }

$info = (Get-Item $outputFullPath).VersionInfo
$expectedProductVersion = "$Version.0"
if ([string]$info.ProductName -ne 'Ghosium Browser' -or
    [string]$info.CompanyName -ne 'Brendigo' -or
    [string]$info.FileDescription -ne 'Ghosium Browser' -or
    [string]$info.OriginalFilename -ne 'Ghosium-Browser.exe' -or
    [string]$info.ProductVersion -ne $expectedProductVersion) {
  throw 'Ghosium launcher Windows version metadata verification failed.'
}

Remove-Item $resPath -Force -ErrorAction SilentlyContinue
Write-Host 'Ghosium C++20 launcher build, icon and Windows metadata verification: OK'
