param(
    [string] $EnvPath = (Join-Path $PSScriptRoot "..\.env")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function New-SecretValue {
    $bytes = [byte[]]::new(32)
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

$resolvedEnvPath = [System.IO.Path]::GetFullPath($EnvPath)
$envDirectory = Split-Path -Parent $resolvedEnvPath

if (-not (Test-Path -LiteralPath $envDirectory)) {
    New-Item -ItemType Directory -Force -Path $envDirectory | Out-Null
}

if (-not (Test-Path -LiteralPath $resolvedEnvPath)) {
    New-Item -ItemType File -Path $resolvedEnvPath | Out-Null
}

$envLines = Get-Content -LiteralPath $resolvedEnvPath
$keyPattern = "^\s*BOOK_IMAGE_ENRICHMENT_KEY\s*="
$keyLine = $envLines | Where-Object { $_ -match $keyPattern } | Select-Object -First 1

if ($null -eq $keyLine) {
    if ($envLines.Count -gt 0 -and $envLines[-1].Trim() -ne "") {
        Add-Content -LiteralPath $resolvedEnvPath -Value ""
    }
    Add-Content -LiteralPath $resolvedEnvPath -Value ("BOOK_IMAGE_ENRICHMENT_KEY=" + (New-SecretValue))
    Write-Host "BOOK_IMAGE_ENRICHMENT_KEY was generated and added to $resolvedEnvPath"
    exit 0
}

$currentValue = ($keyLine -split "=", 2)[1].Trim()
if ([string]::IsNullOrWhiteSpace($currentValue)) {
    $newSecret = New-SecretValue
    $updatedLines = $envLines | ForEach-Object {
        if ($_ -match $keyPattern) {
            "BOOK_IMAGE_ENRICHMENT_KEY=$newSecret"
        } else {
            $_
        }
    }
    Set-Content -LiteralPath $resolvedEnvPath -Value $updatedLines
    Write-Host "BOOK_IMAGE_ENRICHMENT_KEY was generated in $resolvedEnvPath"
    exit 0
}

Write-Host "BOOK_IMAGE_ENRICHMENT_KEY already exists in $resolvedEnvPath"
