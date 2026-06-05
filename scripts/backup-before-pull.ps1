[CmdletBinding()]
param(
  [string]$ServerDir = (Get-Location).Path,
  [string]$BackupRoot = "",
  [switch]$AllowRunning
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-DefaultBackupRoot {
  param([string]$ResolvedServerDir)

  $parent = Split-Path -Parent $ResolvedServerDir
  if ((Split-Path -Leaf $parent) -eq "current") {
    return Join-Path (Split-Path -Parent $parent) "backups\pre-pull"
  }

  return Join-Path $parent "backups\pre-pull"
}

function Get-ServerJavaProcesses {
  param([string]$ResolvedServerDir)

  $escapedDir = [regex]::Escape($ResolvedServerDir)
  Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" |
    Where-Object { $_.CommandLine -and ($_.CommandLine -match $escapedDir) }
}

function Get-ReleaseName {
  param([string]$ResolvedServerDir)

  $manifest = Join-Path $ResolvedServerDir "VERSION_MANIFEST.md"
  if (Test-Path -LiteralPath $manifest) {
    foreach ($line in Get-Content -LiteralPath $manifest) {
      if ($line -match 'Server release:\s*`?([^`\r\n]+)`?') {
        return ($Matches[1].Trim() -replace "[^\w\.\-]+", "-")
      }
    }
  }

  return "unknown-release"
}

$ServerDir = (Resolve-Path -LiteralPath $ServerDir).Path
if ([string]::IsNullOrWhiteSpace($BackupRoot)) {
  $BackupRoot = Get-DefaultBackupRoot -ResolvedServerDir $ServerDir
}

if (-not $AllowRunning) {
  $processes = @(Get-ServerJavaProcesses -ResolvedServerDir $ServerDir)
  if ($processes.Count -gt 0) {
    $pids = ($processes | ForEach-Object { $_.ProcessId }) -join ", "
    throw "Server appears to be running for '$ServerDir'. Stop it first. Java PIDs: $pids"
  }
}

$release = Get-ReleaseName -ResolvedServerDir $ServerDir
$stamp = Get-Date -Format "yyyy-MM-dd_HHmmss"
$backupName = "${stamp}_${release}_pre-pull"
$BackupRoot = [System.IO.Path]::GetFullPath($BackupRoot)
$zipPath = Join-Path $BackupRoot "$backupName.zip"
$shaPath = Join-Path $BackupRoot "$backupName.sha256"
$notePath = Join-Path $BackupRoot "$backupName.txt"
$stageRoot = Join-Path ([System.IO.Path]::GetTempPath()) "leafmc-backup-$backupName"

New-Item -ItemType Directory -Force -Path $BackupRoot | Out-Null
if (Test-Path -LiteralPath $stageRoot) {
  Remove-Item -LiteralPath $stageRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $stageRoot | Out-Null

$include = @(
  ".gitattributes",
  ".gitignore",
  "README.md",
  "RUNBOOK-GIT.md",
  "VERSION_MANIFEST.md",
  "CHANGELOG.md",
  "CHECKSUMS.txt",
  "start-windows.bat",
  "start.sh",
  "server.properties",
  "bukkit.yml",
  "spigot.yml",
  "purpur.yml",
  "permissions.yml",
  "commands.yml",
  "help.yml",
  "config",
  "plugins",
  "world",
  "world_nether",
  "world_the_end"
)

foreach ($item in $include) {
  $src = Join-Path $ServerDir $item
  if (Test-Path -LiteralPath $src) {
    $dst = Join-Path $stageRoot $item
    $dstParent = Split-Path -Parent $dst
    if (-not (Test-Path -LiteralPath $dstParent)) {
      New-Item -ItemType Directory -Force -Path $dstParent | Out-Null
    }
    Copy-Item -LiteralPath $src -Destination $dst -Recurse -Force
  }
}

if (Test-Path -LiteralPath $zipPath) {
  Remove-Item -LiteralPath $zipPath -Force
}

Compress-Archive -Path (Join-Path $stageRoot "*") -DestinationPath $zipPath -CompressionLevel Optimal
$hash = Get-FileHash -LiteralPath $zipPath -Algorithm SHA256
"{0}  {1}" -f $hash.Hash.ToLowerInvariant(), (Split-Path -Leaf $zipPath) | Set-Content -LiteralPath $shaPath -Encoding ASCII

@(
  "created=$(Get-Date -Format s)",
  "source=$ServerDir",
  "release=$release",
  "backup=$zipPath",
  "sha256=$($hash.Hash.ToLowerInvariant())",
  "reason=pre-pull"
) | Set-Content -LiteralPath $notePath -Encoding UTF8

Remove-Item -LiteralPath $stageRoot -Recurse -Force

Write-Host "Backup created:"
Write-Host "  $zipPath"
Write-Host "  $shaPath"
