[CmdletBinding()]
param(
  [string]$ServerDir = (Get-Location).Path,
  [string]$ChecksumFile = "CHECKSUMS.txt"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ServerDir = (Resolve-Path -LiteralPath $ServerDir).Path
$checksumPath = Join-Path $ServerDir $ChecksumFile

if (-not (Test-Path -LiteralPath $checksumPath)) {
  throw "Checksum file not found: $checksumPath"
}

$failures = New-Object System.Collections.Generic.List[string]

foreach ($line in Get-Content -LiteralPath $checksumPath) {
  $trimmed = $line.Trim()
  if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
    continue
  }

  $parts = $trimmed -split "\s+", 2
  if ($parts.Count -ne 2) {
    $failures.Add("Invalid checksum line: $line")
    continue
  }

  $expected = $parts[0].ToLowerInvariant()
  $relative = $parts[1].Trim()
  $filePath = Join-Path $ServerDir ($relative -replace "/", [System.IO.Path]::DirectorySeparatorChar)

  if (-not (Test-Path -LiteralPath $filePath)) {
    $failures.Add("Missing file: $relative")
    continue
  }

  $actual = (Get-FileHash -LiteralPath $filePath -Algorithm SHA256).Hash.ToLowerInvariant()
  if ($actual -ne $expected) {
    $failures.Add("Checksum mismatch: $relative expected=$expected actual=$actual")
  }
}

if ($failures.Count -gt 0) {
  $failures | ForEach-Object { Write-Host $_ }
  throw "Checksum verification failed."
}

Write-Host "Checksum verification passed."
