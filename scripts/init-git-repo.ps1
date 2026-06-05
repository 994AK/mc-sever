[CmdletBinding()]
param(
  [string]$ServerDir = (Get-Location).Path,
  [string]$RemoteUrl = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ServerDir = (Resolve-Path -LiteralPath $ServerDir).Path
Set-Location -LiteralPath $ServerDir

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
  throw "Git is not installed or not in PATH."
}

if (-not (Test-Path -LiteralPath (Join-Path $ServerDir ".git"))) {
  git init
  git branch -M main
}

git add .

Write-Host ""
Write-Host "Review the staged files below before committing."
Write-Host "There should be no world folders, logs, cache, libraries, versions, or database files."
Write-Host ""
git status --short

if (-not [string]::IsNullOrWhiteSpace($RemoteUrl)) {
  $existingOrigin = git remote get-url origin 2>$null
  if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($existingOrigin)) {
    git remote add origin $RemoteUrl
  } else {
    Write-Host "origin already exists: $existingOrigin"
  }
}

Write-Host ""
Write-Host "If the staged list is correct, run:"
Write-Host "  git commit -m `"chore: initial leafmc server package`""
if (-not [string]::IsNullOrWhiteSpace($RemoteUrl)) {
  Write-Host "  git push -u origin main"
}
