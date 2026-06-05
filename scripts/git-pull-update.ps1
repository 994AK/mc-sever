[CmdletBinding()]
param(
  [string]$ServerDir = (Get-Location).Path,
  [switch]$NoBackup,
  [switch]$AllowRunning,
  [switch]$SkipChecksum
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ServerDir = (Resolve-Path -LiteralPath $ServerDir).Path
Set-Location -LiteralPath $ServerDir

if (-not (Test-Path -LiteralPath (Join-Path $ServerDir ".git"))) {
  throw "This directory is not a Git repository: $ServerDir"
}

$dirty = git status --porcelain
if ($dirty) {
  Write-Host $dirty
  throw "Working tree is not clean. Commit, stash, or discard local tracked changes before pulling."
}

if (-not $NoBackup) {
  & (Join-Path $PSScriptRoot "backup-before-pull.ps1") -ServerDir $ServerDir -AllowRunning:$AllowRunning
} else {
  Write-Warning "Skipping backup because -NoBackup was provided."
}

git pull --ff-only

$lfsVersion = $null
try {
  $lfsVersion = git lfs version 2>$null
} catch {
  $lfsVersion = $null
}

if ($lfsVersion) {
  git lfs pull
}

if (-not $SkipChecksum) {
  & (Join-Path $PSScriptRoot "verify-checksums.ps1") -ServerDir $ServerDir
}

Write-Host ""
Write-Host "Pull complete. Start the server with:"
Write-Host "  .\start-windows.bat"
