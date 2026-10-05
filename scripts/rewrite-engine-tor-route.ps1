param(
  [Parameter(Mandatory = $true)]
  [string]$SourceRoot
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$expectedRevision = (Get-Content (Join-Path $repoRoot 'ENGINE_SOURCE_REVISION') -Raw).Trim()
$contract = Get-Content (Join-Path $repoRoot 'engine/tor/windows-x64.json') -Raw | ConvertFrom-Json
$sourceRootResolved = (Resolve-Path $SourceRoot).Path

$actualRevision = (& git -C $sourceRootResolved rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or $actualRevision -ne $expectedRevision) {
  throw "Tor integration requires pinned Chromium $expectedRevision; found $actualRevision"
}

$target = Join-Path $sourceRootResolved 'chrome/app/chrome_main_delegate.cc'
if (!(Test-Path $target -PathType Leaf)) {
  throw "Pinned Chromium startup source is missing: $target"
}

$text = [IO.File]::ReadAllText($target)
$marker = 'Ghosium integrated Tor route'
if ($text.Contains($marker)) {
  Write-Host 'Ghosium integrated Tor route already applied.'
  exit 0
}

$includeAnchor = '#include "base/process/memory.h"'
if (!$text.Contains($includeAnchor)) {
  throw 'Pinned Chromium process include anchor changed.'
}
$text = $text.Replace(
  $includeAnchor,
  $includeAnchor + [Environment]::NewLine + '#include "base/process/launch.h"'
)

$helperAnchor = 'bool IsCanaryDev() {'
if (!$text.Contains($helperAnchor)) {
  throw 'Pinned Chromium startup helper anchor changed.'
}

$socksEndpoint = [string]$contract.socksEndpoint
$controlEndpoint = [string]$contract.controlEndpoint
if ($socksEndpoint -notmatch '^127\.0\.0\.1:\d+$' -or
    $controlEndpoint -notmatch '^127\.0\.0\.1:\d+$') {
  throw 'Pinned Tor loopback endpoint contract is invalid.'
}

$helper = @"
#if BUILDFLAG(IS_WIN) && !defined(BUILDING_CHROME_RENDERER)
// Ghosium integrated Tor route: one browser executable with an isolated
// Tor-routed profile. Direct browsing remains the normal Ghosium path.
constexpr char kGhosiumTorSwitch[] = "ghosium-tor";
constexpr char kGhosiumTorProxy[] = "socks5://$socksEndpoint";
constexpr char kGhosiumTorResolverRules[] =
    "MAP * ~NOTFOUND , EXCLUDE 127.0.0.1";

bool ConfigureAndLaunchGhosiumTor(base::CommandLine* command_line) {
  if (!command_line->HasSwitch(kGhosiumTorSwitch)) {
    return true;
  }

  // A Tor-routed Ghosium instance is fail-closed. User-provided proxy/PAC
  // settings are rejected instead of being allowed to bypass the Tor route.
  if (command_line->HasSwitch("proxy-server") ||
      command_line->HasSwitch("proxy-pac-url")) {
    LOG(ERROR) << "Ghosium Tor route rejects external proxy overrides.";
    return false;
  }

  base::FilePath executable_dir;
  if (!base::PathService::Get(base::DIR_EXE, &executable_dir)) {
    LOG(ERROR) << "Ghosium Tor route could not resolve the browser directory.";
    return false;
  }

  const base::FilePath tor_executable =
      executable_dir.Append(FILE_PATH_LITERAL("Tor"))
          .Append(FILE_PATH_LITERAL("tor.exe"));
  if (!base::PathExists(tor_executable)) {
    LOG(ERROR) << "Ghosium Tor runtime is missing.";
    return false;
  }

  base::FilePath base_profile;
  if (command_line->HasSwitch(switches::kUserDataDir)) {
    base_profile = command_line->GetSwitchValuePath(switches::kUserDataDir);
  } else if (!chrome::GetDefaultUserDataDirectory(&base_profile)) {
    LOG(ERROR) << "Ghosium Tor route could not resolve the user-data root.";
    return false;
  }

  // Never share the Direct profile with the Tor route. This is still the same
  // Ghosium application and executable; only identity-bearing browser state is
  // isolated internally.
  const base::FilePath tor_profile =
      base_profile.DirName().Append(FILE_PATH_LITERAL("Tor User Data"));
  const base::FilePath tor_data =
      base_profile.DirName().Append(FILE_PATH_LITERAL("Tor Runtime Data"));
  if (!base::CreateDirectory(tor_profile) || !base::CreateDirectory(tor_data)) {
    LOG(ERROR) << "Ghosium Tor route could not prepare isolated local data.";
    return false;
  }

  command_line->RemoveSwitch(switches::kUserDataDir);
  command_line->AppendSwitchPath(switches::kUserDataDir, tor_profile);
  command_line->AppendSwitchASCII("proxy-server", kGhosiumTorProxy);
  command_line->AppendSwitchASCII("host-resolver-rules",
                                  kGhosiumTorResolverRules);
  command_line->AppendSwitch("disable-quic");
  command_line->AppendSwitch("disable-background-networking");
  command_line->AppendSwitch("incognito");
  command_line->AppendSwitchASCII("force-webrtc-ip-handling-policy",
                                  "disable_non_proxied_udp");

  base::CommandLine tor_command(tor_executable);
  tor_command.AppendArg("--SocksPort");
  tor_command.AppendArg("$socksEndpoint");
  tor_command.AppendArg("--ControlPort");
  tor_command.AppendArg("$controlEndpoint");
  tor_command.AppendArg("--CookieAuthentication");
  tor_command.AppendArg("1");
  tor_command.AppendArg("--ClientOnly");
  tor_command.AppendArg("1");
  tor_command.AppendArg("--AvoidDiskWrites");
  tor_command.AppendArg("1");
  tor_command.AppendArg("--SafeSocks");
  tor_command.AppendArg("1");
  tor_command.AppendArg("--DataDirectory");
  tor_command.AppendArgPath(tor_data);
  tor_command.AppendArg("__OwningControllerProcess");
  tor_command.AppendArg(std::to_string(base::GetCurrentProcId()));

  base::Process tor_process =
      base::LaunchProcess(tor_command, base::LaunchOptions());
  if (!tor_process.IsValid()) {
    LOG(ERROR) << "Ghosium could not launch its pinned Tor runtime.";
    return false;
  }

  // The Tor daemon owns its lifetime through __OwningControllerProcess and
  // exits after the Ghosium browser process disappears.
  return true;
}
#endif  // BUILDFLAG(IS_WIN) && !defined(BUILDING_CHROME_RENDERER)

"@

$text = $text.Replace($helperAnchor, $helper + $helperAnchor)

$browserAnchor = '  const bool is_browser = !command_line.HasSwitch(switches::kProcessType);'
if (!$text.Contains($browserAnchor)) {
  throw 'Pinned Chromium browser-process startup anchor changed.'
}

$activation = @"
$browserAnchor
#if BUILDFLAG(IS_WIN)
  if (is_browser &&
      !ConfigureAndLaunchGhosiumTor(base::CommandLine::ForCurrentProcess())) {
    return CHROME_RESULT_CODE_MISSING_DATA;
  }
#endif
"@
$text = $text.Replace($browserAnchor, $activation)

foreach ($required in @(
  $marker,
  'kGhosiumTorSwitch[] = "ghosium-tor"',
  'socks5://$socksEndpoint',
  'Tor User Data',
  'Tor Runtime Data',
  'host-resolver-rules',
  'disable_non_proxied_udp',
  '__OwningControllerProcess',
  'CHROME_RESULT_CODE_MISSING_DATA'
)) {
  if (!$text.Contains($required)) {
    throw "Ghosium Tor source rewrite lost required contract token: $required"
  }
}

[IO.File]::WriteAllText($target, $text, [Text.UTF8Encoding]::new($false))

$thirdPartyChanges = & git -C $sourceRootResolved status --porcelain=v1 -- third_party
if ($LASTEXITCODE -ne 0) {
  throw 'Unable to verify third_party state after Tor source integration.'
}
if ($thirdPartyChanges) {
  throw 'Ghosium Tor integration modified third_party source.'
}

Write-Host 'Ghosium single-browser Tor route applied to pinned Windows engine source.'
