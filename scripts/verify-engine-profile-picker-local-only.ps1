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
  throw "Profile Picker verification requires pinned source $expectedRevision; found $actualRevision"
}

$profilePickerUi = Join-Path $sourceRootResolved 'chrome/browser/ui/webui/signin/profile_picker_ui.cc'
if (!(Test-Path $profilePickerUi -PathType Leaf)) {
  throw "Required Profile Picker source is missing: $profilePickerUi"
}

$text = [IO.File]::ReadAllText($profilePickerUi)
foreach ($required in @(
  'html_source->AddBoolean("signInProfileCreationFlowSupported", false);',
  'html_source->AddBoolean("isBrowserSigninAllowed", false);'
)) {
  if (!$text.Contains($required)) {
    throw "Ghosium local-only Profile Picker contract is missing: $required"
  }
}

# Chromium 155 exposes force-signin before the Glic branch and again in the
# normal picker branch. Ghosium must force both public paths to static false.
$forceSigninFalseCount = [regex]::Matches(
  $text,
  'html_source->AddBoolean\("isForceSigninEnabled", false\);'
).Count
if ($forceSigninFalseCount -ne 2) {
  throw "Profile Picker force-signin contract expects exactly two static false gates; found $forceSigninFalseCount."
}
if ($text -match 'signin_util::IsForceSigninEnabled\(\)') {
  throw 'A dynamic Chromium force-signin gate remains in the Ghosium Profile Picker.'
}

foreach ($forbidden in @(
  'html_source->AddBoolean("signInProfileCreationFlowSupported",`n                          AccountConsistencyModeManager::IsDiceSignInAllowed());',
  'html_source->AddBoolean("isBrowserSigninAllowed", IsBrowserSigninAllowed());',
  'signin_util::IsForceSigninEnabled());'
)) {
  if ($text.Contains($forbidden)) {
    throw "An upstream browser-account Profile Picker gate can still become public: $forbidden"
  }
}

$thirdPartyChanges = & git -C $sourceRootResolved status --porcelain=v1 -- third_party
if ($LASTEXITCODE -ne 0) {
  throw 'Unable to verify third_party state during Profile Picker audit.'
}
if ($thirdPartyChanges) {
  throw 'Profile Picker audit detected third_party modifications.'
}

Write-Host 'Ghosium Profile Picker audit: profile creation is local-only; browser sign-in and force-signin onboarding are unavailable.'
