param(
  [Parameter(Mandatory = $true)]
  [string]$SourceRoot
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$expectedRevision = (Get-Content (Join-Path $repoRoot 'ENGINE_SOURCE_REVISION') -Raw).Trim()
$sourceRootResolved = (Resolve-Path $SourceRoot).Path
$actualRevision = (& git -C $sourceRootResolved rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or $actualRevision -ne $expectedRevision) {
  throw "Tor menu integration requires pinned Chromium $expectedRevision; found $actualRevision"
}

$commandIds = Join-Path $sourceRootResolved 'chrome/app/chrome_command_ids.h'
$appMenu = Join-Path $sourceRootResolved 'chrome/browser/ui/toolbar/app_menu_model.cc'
foreach ($required in @($commandIds, $appMenu)) {
  if (!(Test-Path $required -PathType Leaf)) {
    throw "Pinned Chromium Tor-menu source is missing: $required"
  }
}

$idText = [IO.File]::ReadAllText($commandIds)
$menuText = [IO.File]::ReadAllText($appMenu)

if ($idText.Contains('IDC_NEW_GHOSIUM_TOR_WINDOW') -and
    $menuText.Contains('Ghosium native Tor menu entry')) {
  Write-Host 'Ghosium native Tor menu entry already applied.'
  exit 0
}

if ($idText.Contains('IDC_NEW_GHOSIUM_TOR_WINDOW') -or
    $menuText.Contains('Ghosium native Tor menu entry')) {
  throw 'Partial Ghosium Tor-menu transform detected.'
}

$idAnchor = '#define IDC_CYCLE_TO_PREV_TAB           34063'
if (!$idText.Contains($idAnchor) -or $idText.Contains('34064')) {
  throw 'Pinned Chromium command-id allocation changed; 34064 is not safely available.'
}
$idText = $idText.Replace(
  $idAnchor,
  $idAnchor + [Environment]::NewLine +
  '#define IDC_NEW_GHOSIUM_TOR_WINDOW       34064'
)

$includeAnchor = '#include "base/memory/raw_ptr.h"'
if (!$menuText.Contains($includeAnchor)) {
  throw 'Pinned Chromium app-menu include anchor changed.'
}
$menuText = $menuText.Replace(
  $includeAnchor,
  $includeAnchor + [Environment]::NewLine + '#include "base/process/launch.h"'
)

$executeAnchor = @"
  if (command_id == IDC_VIEW_PASSWORDS) {
    browser()->GetProfile()->GetPrefs()->SetBoolean(
        password_manager::prefs::kPasswordsPrefWithNewLabelUsed, true);
  }

  LogMenuMetrics(command_id);
"@
if (!$menuText.Contains($executeAnchor)) {
  throw 'Pinned Chromium app-menu execute anchor changed.'
}

$executeReplacement = @"
  if (command_id == IDC_VIEW_PASSWORDS) {
    browser()->GetProfile()->GetPrefs()->SetBoolean(
        password_manager::prefs::kPasswordsPrefWithNewLabelUsed, true);
  }

  // Ghosium native Tor menu entry. Launch the same browser executable with the
  // integrated Tor route; this is not a second browser product.
  if (command_id == IDC_NEW_GHOSIUM_TOR_WINDOW) {
    base::CommandLine tor_command(
        base::CommandLine::ForCurrentProcess()->GetProgram());
    tor_command.AppendSwitch("ghosium-tor");
    if (base::CommandLine::ForCurrentProcess()->HasSwitch(
            switches::kUserDataDir)) {
      tor_command.AppendSwitchPath(
          switches::kUserDataDir,
          base::CommandLine::ForCurrentProcess()->GetSwitchValuePath(
              switches::kUserDataDir));
    }
    if (base::CommandLine::ForCurrentProcess()->HasSwitch(switches::kLang)) {
      tor_command.AppendSwitchASCII(
          switches::kLang,
          base::CommandLine::ForCurrentProcess()->GetSwitchValueASCII(
              switches::kLang));
    }
    base::LaunchProcess(tor_command, base::LaunchOptions());
    return;
  }

  LogMenuMetrics(command_id);
"@
$menuText = $menuText.Replace($executeAnchor, $executeReplacement)

$enabledAnchor = @"
  switch (command_id) {
    case IDC_NEW_INCOGNITO_WINDOW:
      return IncognitoModePrefs::IsIncognitoAllowed(browser_->GetProfile());
    default:
      return chrome::IsCommandEnabled(browser_, command_id);
  }
"@
if (!$menuText.Contains($enabledAnchor)) {
  throw 'Pinned Chromium app-menu enabled-state anchor changed.'
}
$enabledReplacement = @"
  switch (command_id) {
    case IDC_NEW_INCOGNITO_WINDOW:
      return IncognitoModePrefs::IsIncognitoAllowed(browser_->GetProfile());
    case IDC_NEW_GHOSIUM_TOR_WINDOW:
      return !browser_->GetProfile()->IsGuestSession();
    default:
      return chrome::IsCommandEnabled(browser_, command_id);
  }
"@
$menuText = $menuText.Replace($enabledAnchor, $enabledReplacement)

$buildAnchor = @"
    SetElementIdentifierAt(
        GetIndexOfCommandId(IDC_NEW_INCOGNITO_WINDOW).value(),
        kIncognitoMenuItem);

    bool isolated_mode_enabled =
"@
if (!$menuText.Contains($buildAnchor)) {
  throw 'Pinned Chromium app-menu build anchor changed.'
}
$buildReplacement = @"
    SetElementIdentifierAt(
        GetIndexOfCommandId(IDC_NEW_INCOGNITO_WINDOW).value(),
        kIncognitoMenuItem);

    AddItemWithIcon(
        IDC_NEW_GHOSIUM_TOR_WINDOW, u"New Tor window",
        ui::ImageModel::FromVectorIcon(
            features::IsRoundedIconsEnabled() ? kNewWindowIcon
                                              : kNewWindowOldIcon,
            ui::kColorMenuIcon, ui::SimpleMenuModel::kDefaultIconSize));

    bool isolated_mode_enabled =
"@
$menuText = $menuText.Replace($buildAnchor, $buildReplacement)

foreach ($token in @(
  'IDC_NEW_GHOSIUM_TOR_WINDOW       34064',
  'Ghosium native Tor menu entry',
  'tor_command.AppendSwitch("ghosium-tor")',
  'u"New Tor window"',
  'base::LaunchProcess(tor_command, base::LaunchOptions())'
)) {
  if (!$idText.Contains($token) -and !$menuText.Contains($token)) {
    throw "Ghosium Tor-menu transform lost required token: $token"
  }
}

[IO.File]::WriteAllText($commandIds, $idText, [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText($appMenu, $menuText, [Text.UTF8Encoding]::new($false))

$thirdPartyChanges = & git -C $sourceRootResolved status --porcelain=v1 -- third_party
if ($LASTEXITCODE -ne 0) {
  throw 'Unable to verify third_party state after Tor-menu integration.'
}
if ($thirdPartyChanges) {
  throw 'Ghosium Tor-menu integration modified third_party source.'
}

Write-Host 'Ghosium native New Tor window menu entry applied to the existing browser UI.'
