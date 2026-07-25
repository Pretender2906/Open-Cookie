# Generates android/website/.well-known/assetlinks.json for Mobile Wallet Adapter
# domain verification (Digital Asset Links).
#
# Usage (debug keystore — local builds):
#   .\scripts\generate-assetlinks.ps1
#
# Usage (release keystore):
#   .\scripts\generate-assetlinks.ps1 `
#     -Keystore "C:\path\to\release.keystore" `
#     -Alias "opencookie" `
#     -StorePass "secret" `
#     -Append
#
# Play Store: use the App signing certificate SHA-256 from Play Console
# (Release > Setup > App signing), not necessarily your upload key.
# Pass it with -Fingerprint "AA:BB:..." -Append

param(
    [string]$Keystore = "$env:USERPROFILE\.android\debug.keystore",
    [string]$Alias = "androiddebugkey",
    [string]$StorePass = "android",
    [string]$KeyPass = "android",
    [string[]]$Fingerprint = @(),
    [switch]$Append,
    [string]$Output = "$PSScriptRoot\..\android\website\.well-known\assetlinks.json"
)

$ErrorActionPreference = "Stop"

function Find-Keytool {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\keytool.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    $studioKeytool = "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
    if (Test-Path $studioKeytool) { return $studioKeytool }
    throw "keytool not found. Set JAVA_HOME or install Android Studio."
}

function Get-Sha256Fingerprint {
    param([string]$Keytool, [string]$Path, [string]$AliasName, [string]$StorePassword, [string]$KeyPassword)
    if (-not (Test-Path $Path)) {
        throw "Keystore not found: $Path"
    }
    $output = & $Keytool -list -v `
        -keystore $Path `
        -alias $AliasName `
        -storepass $StorePassword `
        -keypass $KeyPassword 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "keytool failed:`n$output"
    }
    $line = $output | Where-Object { $_ -match "^\s*SHA256:" } | Select-Object -First 1
    if (-not $line) {
        throw "SHA256 fingerprint not found in keytool output."
    }
    return ($line -replace "^\s*SHA256:\s*", "").Trim()
}

function Read-ExistingFingerprints {
    param([string]$Path, [string]$PackageName)
    if (-not (Test-Path $Path)) { return @() }
    $json = Get-Content $Path -Raw | ConvertFrom-Json
    foreach ($entry in $json) {
        if ($entry.target.package_name -eq $PackageName) {
            return @($entry.target.sha256_cert_fingerprints)
        }
    }
    return @()
}

$keytool = Find-Keytool
$newFingerprints = @()

if ($Fingerprint.Count -gt 0) {
    $newFingerprints += $Fingerprint
} else {
    Write-Host "Reading SHA-256 from keystore: $Keystore"
    $newFingerprints += Get-Sha256Fingerprint -Keytool $keytool -Path $Keystore -AliasName $Alias -StorePassword $StorePass -KeyPassword $KeyPass
}

$packages = @("com.opencookie.app", "com.opencookie.admin")
$entries = @()

foreach ($package in $packages) {
    $fingerprints = @($newFingerprints)
    if ($Append -and (Test-Path $Output)) {
        $existing = Read-ExistingFingerprints -Path $Output -PackageName $package
        $fingerprints = @($existing) + @($newFingerprints) | Select-Object -Unique
    }

    $entries += [ordered]@{
        relation = @("delegate_permission/common.handle_all_urls")
        target   = [ordered]@{
            namespace                 = "android_app"
            package_name              = $package
            sha256_cert_fingerprints  = $fingerprints
        }
    }
}

$outputDir = Split-Path $Output -Parent
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

$json = ($entries | ConvertTo-Json -Depth 6) -replace '\\u0026', '&'
# PowerShell collapses single-element arrays into strings; DAL requires a JSON array.
$json = $json -replace '"sha256_cert_fingerprints":\s*"([^"]+)"', '"sha256_cert_fingerprints": ["$1"]'
[System.IO.File]::WriteAllText($Output, $json + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))

Write-Host ""
Write-Host "Wrote $Output"
Write-Host "Fingerprints:"
foreach ($fp in ($entries[0].target.sha256_cert_fingerprints)) {
    Write-Host "  $fp"
}
Write-Host ""
Write-Host "Next: commit, push, redeploy Cloudflare Pages, then verify:"
Write-Host "  https://open-cookie.pages.dev/.well-known/assetlinks.json"
