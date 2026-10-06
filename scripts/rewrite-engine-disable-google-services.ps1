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
  throw "Google-service hardening requires pinned Chromium $expectedRevision; found $actualRevision"
}

function Replace-RequiredLiteral {
  param(
    [Parameter(Mandatory = $true)][string]$Path,
    [Parameter(Mandatory = $true)][string]$OldValue,
    [Parameter(Mandatory = $true)][string]$NewValue,
    [Parameter(Mandatory = $true)][string]$Description
  )
  if (!(Test-Path $Path -PathType Leaf)) {
    throw "Required source file is missing for $($Description): $Path"
  }
  $text = [IO.File]::ReadAllText($Path)
  if ($text.Contains($OldValue)) {
    [IO.File]::WriteAllText($Path, $text.Replace($OldValue, $NewValue), [Text.UTF8Encoding]::new($false))
    Write-Host "Applied: $Description"
    return
  }
  if (!$text.Contains($NewValue)) {
    throw "Pinned source anchor changed for $($Description): $Path"
  }
}

function Replace-RequiredRegex {
  param(
    [Parameter(Mandatory = $true)][string]$Path,
    [Parameter(Mandatory = $true)][string]$Pattern,
    [Parameter(Mandatory = $true)][string]$Replacement,
    [Parameter(Mandatory = $true)][string]$AlreadyPresent,
    [Parameter(Mandatory = $true)][string]$Description
  )
  if (!(Test-Path $Path -PathType Leaf)) {
    throw "Required source file is missing for $($Description): $Path"
  }
  $text = [IO.File]::ReadAllText($Path)
  if ($text.Contains($AlreadyPresent)) {
    return
  }
  $regex = [regex]::new($Pattern, [Text.RegularExpressions.RegexOptions]::Singleline)
  $matches = $regex.Matches($text)
  if ($matches.Count -ne 1) {
    throw "Pinned source anchor changed for $($Description); expected one match, found $($matches.Count): $Path"
  }
  $updated = $regex.Replace($text, $Replacement, 1)
  [IO.File]::WriteAllText($Path, $updated, [Text.UTF8Encoding]::new($false))
  Write-Host "Applied: $Description"
}

$gcm = Join-Path $sourceRootResolved 'components/gcm_driver/gcm_driver_desktop.cc'
$domainReliability = Join-Path $sourceRootResolved 'chrome/browser/domain_reliability/service_factory.cc'
$networkTime = Join-Path $sourceRootResolved 'components/network_time/network_time_tracker.cc'
$variationsService = Join-Path $sourceRootResolved 'components/variations/service/variations_service.cc'
$variationsHeaders = Join-Path $sourceRootResolved 'components/variations/net/variations_http_headers.cc'
$crashReporter = Join-Path $sourceRootResolved 'components/crash/core/app/crash_reporter_client.cc'
$webrtcUploader = Join-Path $sourceRootResolved 'chrome/browser/media/webrtc/webrtc_log_uploader.cc'
$browserPrefs = Join-Path $sourceRootResolved 'chrome/browser/ui/browser_ui_prefs.cc'
$autofill = Join-Path $sourceRootResolved 'components/autofill/core/browser/crowdsourcing/autofill_crowdsourcing_manager.cc'

$gcmReplacement = @'
GCMClient::Result GCMDriverDesktop::EnsureStarted(
    GCMClient::StartMode start_mode) {
  // Ghosium privacy: Google Cloud Messaging is not a browser dependency.
  (void)start_mode;
  return GCMClient::GCM_DISABLED;
}

void GCMDriverDesktop::RemoveCachedData
'@
Replace-RequiredRegex -Path $gcm -Pattern 'GCMClient::Result GCMDriverDesktop::EnsureStarted\(\s*GCMClient::StartMode start_mode\) \{.*?\n\}\n\nvoid GCMDriverDesktop::RemoveCachedData' -Replacement $gcmReplacement -AlreadyPresent 'Ghosium privacy: Google Cloud Messaging is not a browser dependency.' -Description 'Google Cloud Messaging disablement'

$domainReplacement = @'
bool ShouldCreateService(const DomainReliabilityServiceDelegate* delegate) {
  // Ghosium privacy: never create the background Domain Reliability uploader.
  (void)delegate;
  return false;
}

}  // namespace domain_reliability
'@
Replace-RequiredRegex -Path $domainReliability -Pattern 'bool ShouldCreateService\(const DomainReliabilityServiceDelegate\* delegate\) \{.*?\n\}\n\n\}  // namespace domain_reliability' -Replacement $domainReplacement -AlreadyPresent 'Ghosium privacy: never create the background Domain Reliability uploader.' -Description 'Domain Reliability disablement'

$networkTimeReplacement = @'
bool NetworkTimeTracker::AreTimeFetchesEnabled() const {
  // Ghosium privacy: never query a browser-owned remote network-time service.
  return false;
}
'@
Replace-RequiredRegex -Path $networkTime -Pattern 'bool NetworkTimeTracker::AreTimeFetchesEnabled\(\) const \{.*?\n\}' -Replacement $networkTimeReplacement -AlreadyPresent 'Ghosium privacy: never query a browser-owned remote network-time service.' -Description 'remote network-time disablement'

$variationsReplacement = @'
bool IsFetchingEnabled() {
  // Ghosium privacy: no remote variations/field-trial seed fetching.
  return false;
}

// Returns the already downloaded first run seed
'@
Replace-RequiredRegex -Path $variationsService -Pattern 'bool IsFetchingEnabled\(\) \{.*?\n\}\n\n// Returns the already downloaded first run seed' -Replacement $variationsReplacement -AlreadyPresent 'Ghosium privacy: no remote variations/field-trial seed fetching.' -Description 'remote variations seed disablement'

$headerReplacement = @'
bool ShouldAppendVariationsHeader(const GURL& url, InIncognito incognito) {
  // Ghosium privacy: never attach experiment identifiers to web requests.
  (void)url;
  (void)incognito;
  return false;
}
'@
Replace-RequiredRegex -Path $variationsHeaders -Pattern 'bool ShouldAppendVariationsHeader\(const GURL& url, InIncognito incognito\) \{.*?\n\}' -Replacement $headerReplacement -AlreadyPresent 'Ghosium privacy: never attach experiment identifiers to web requests.' -Description 'X-Client-Data variations header disablement'

$crashReplacement = @'
std::string CrashReporterClient::GetUploadUrl() {
  // Ghosium privacy: crash data is never uploaded to an upstream endpoint.
  return std::string();
}
'@
Replace-RequiredRegex -Path $crashReporter -Pattern 'std::string CrashReporterClient::GetUploadUrl\(\) \{.*?\n\}' -Replacement $crashReplacement -AlreadyPresent 'Ghosium privacy: crash data is never uploaded to an upstream endpoint.' -Description 'upstream crash upload disablement'

$webrtcReplacement = @'
void WebRtcLogUploader::OnLoggingStopped(
    WebRtcLogUploadSite site,
    std::unique_ptr<WebRtcLogBuffer> log_buffer,
    std::unique_ptr<WebRtcLogMetaDataMap> meta_data,
    WebRtcLogUploader::UploadDoneData upload_done_data,
    bool is_text_log_upload_allowed) {
  DCHECK(background_task_runner_->RunsTasksInCurrentSequence());
  // Ghosium privacy: WebRTC diagnostic data stays off upstream upload paths.
  (void)site;
  (void)log_buffer;
  (void)meta_data;
  (void)is_text_log_upload_allowed;
  main_task_runner_->PostTask(
      FROM_HERE,
      base::BindOnce(&WebRtcLogUploader::NotifyUploadDisabled,
                     base::Unretained(this), std::move(upload_done_data)));
}

void WebRtcLogUploader::PrepareMultipartPostData
'@
Replace-RequiredRegex -Path $webrtcUploader -Pattern 'void WebRtcLogUploader::OnLoggingStopped\(\s*WebRtcLogUploadSite site,.*?\n\}\n\nvoid WebRtcLogUploader::PrepareMultipartPostData' -Replacement $webrtcReplacement -AlreadyPresent 'Ghosium privacy: WebRTC diagnostic data stays off upstream upload paths.' -Description 'WebRTC diagnostic upload disablement'

Replace-RequiredLiteral -Path $browserPrefs -OldValue 'registry->RegisterBooleanPref(prefs::kWebRtcTextLogCollectionAllowed, true);' -NewValue 'registry->RegisterBooleanPref(prefs::kWebRtcTextLogCollectionAllowed, false);' -Description 'WebRTC text-log collection privacy default'

$autofillReplacement = @'
bool AutofillCrowdsourcingManager::StartRequest(FormRequestData request_data) {
  // Ghosium privacy: do not query or upload form structure to Google Autofill.
  (void)request_data;
  return false;
}

void AutofillCrowdsourcingManager::CacheQueryRequest
'@
Replace-RequiredRegex -Path $autofill -Pattern 'bool AutofillCrowdsourcingManager::StartRequest\(FormRequestData request_data\) \{.*?\n\}\n\nvoid AutofillCrowdsourcingManager::CacheQueryRequest' -Replacement $autofillReplacement -AlreadyPresent 'Ghosium privacy: do not query or upload form structure to Google Autofill.' -Description 'Google Autofill crowdsourcing disablement'

foreach ($assertion in @(
  @($gcm, 'GCMClient::GCM_DISABLED'),
  @($domainReliability, 'Ghosium privacy: never create the background Domain Reliability uploader.'),
  @($networkTime, 'Ghosium privacy: never query a browser-owned remote network-time service.'),
  @($variationsService, 'Ghosium privacy: no remote variations/field-trial seed fetching.'),
  @($variationsHeaders, 'Ghosium privacy: never attach experiment identifiers to web requests.'),
  @($crashReporter, 'Ghosium privacy: crash data is never uploaded to an upstream endpoint.'),
  @($webrtcUploader, 'Ghosium privacy: WebRTC diagnostic data stays off upstream upload paths.'),
  @($browserPrefs, 'kWebRtcTextLogCollectionAllowed, false'),
  @($autofill, 'Ghosium privacy: do not query or upload form structure to Google Autofill.')
)) {
  $assertText = [IO.File]::ReadAllText([string]$assertion[0])
  if (!$assertText.Contains([string]$assertion[1])) {
    throw "Google-service hardening verification failed: $($assertion[1])"
  }
}

$thirdPartyChanges = & git -C $sourceRootResolved status --porcelain=v1 -- third_party
if ($LASTEXITCODE -ne 0) {
  throw 'Unable to verify third_party state after Google-service hardening.'
}
if ($thirdPartyChanges) {
  throw 'Google-service hardening modified third_party source.'
}

Write-Host 'Ghosium background Google-service hardening applied: GCM, domain reliability, network time, variations, crash upload, WebRTC upload and Autofill crowdsourcing disabled.'
