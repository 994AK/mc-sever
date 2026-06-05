[CmdletBinding()]
param(
  [string]$ServerDir = (Get-Location).Path,
  [string]$OutputFile = "CHECKSUMS.txt"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ServerDir = (Resolve-Path -LiteralPath $ServerDir).Path
$outputPath = Join-Path $ServerDir $OutputFile

function Get-RelativePathForChecksum {
  param([string]$FullPath)
  $relative = $FullPath.Substring($ServerDir.Length)
  $relative = $relative.TrimStart([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
  return ($relative -replace "\\", "/")
}

$targets = @()
$targets += Get-ChildItem -LiteralPath $ServerDir -Filter "*.jar" -File -ErrorAction SilentlyContinue
$pluginsDir = Join-Path $ServerDir "plugins"
if (Test-Path -LiteralPath $pluginsDir) {
  $targets += Get-ChildItem -LiteralPath $pluginsDir -Filter "*.jar" -File -ErrorAction SilentlyContinue
}

$icon = Join-Path $ServerDir "server-icon.png"
if (Test-Path -LiteralPath $icon) {
  $targets += Get-Item -LiteralPath $icon
}

$motdIconDir = Join-Path $ServerDir "plugins\MiniMOTD\icons"
if (Test-Path -LiteralPath $motdIconDir) {
  $targets += Get-ChildItem -LiteralPath $motdIconDir -Filter "*.png" -File -ErrorAction SilentlyContinue
}

$lines = $targets |
  Sort-Object FullName -Unique |
  ForEach-Object {
    $hash = Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256
    "{0}  {1}" -f $hash.Hash.ToLowerInvariant(), (Get-RelativePathForChecksum -FullPath $_.FullName)
  }

$lines | Set-Content -LiteralPath $outputPath -Encoding ASCII
Write-Host "Wrote $OutputFile"
