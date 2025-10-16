# =============================================================================
# Load Environment Variables from .env file (Windows PowerShell)
# =============================================================================
# Usage:
#   .\scripts\load-env.ps1
#
# Or from project root:
#   powershell -ExecutionPolicy Bypass -File .\scripts\load-env.ps1
# =============================================================================

$envFile = Join-Path $PSScriptRoot "..\..\.env"

if (-not (Test-Path $envFile)) {
    Write-Host "ERROR: .env file not found at $envFile" -ForegroundColor Red
    Write-Host "Copy .env.example to .env and configure your settings" -ForegroundColor Yellow
    exit 1
}

Write-Host "Loading environment variables from .env file..." -ForegroundColor Green
Write-Host ""

$loadedVars = 0
$skippedVars = 0

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()

    # Skip empty lines and comments
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
        return
    }

    # Parse KEY=VALUE
    if ($line -match '^([^=]+)=(.*)$') {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim()

        # Remove quotes if present
        $value = $value.Trim('"').Trim("'")

        # Set environment variable
        [System.Environment]::SetEnvironmentVariable($key, $value, [System.EnvironmentVariableTarget]::Process)

        Write-Host "  ✓ $key" -ForegroundColor Gray
        $loadedVars++
    }
    else {
        Write-Host "  ⚠ Skipped invalid line: $line" -ForegroundColor Yellow
        $skippedVars++
    }
}

Write-Host ""
Write-Host "================================" -ForegroundColor Green
Write-Host "Environment variables loaded:" -ForegroundColor Green
Write-Host "  Loaded: $loadedVars" -ForegroundColor Green
if ($skippedVars -gt 0) {
    Write-Host "  Skipped: $skippedVars" -ForegroundColor Yellow
}
Write-Host "================================" -ForegroundColor Green
Write-Host ""

# Verify critical variables
Write-Host "Verifying critical variables..." -ForegroundColor Cyan

$critical = @{
    "DB_USERNAME" = $env:DB_USERNAME
    "DB_PASSWORD" = $env:DB_PASSWORD
    "JWT_SECRET" = $env:JWT_SECRET
    "MQTT_USERNAME" = $env:MQTT_USERNAME
    "MQTT_PASSWORD" = $env:MQTT_PASSWORD
}

$missingCritical = @()

foreach ($var in $critical.Keys) {
    $value = $critical[$var]
    if ([string]::IsNullOrWhiteSpace($value)) {
        Write-Host "  ✗ $var - NOT SET" -ForegroundColor Red
        $missingCritical += $var
    }
    else {
        $displayValue = if ($value.Length -gt 20) {
            $value.Substring(0, 10) + "..."
        } else {
            $value
        }
        Write-Host "  ✓ $var - SET ($displayValue)" -ForegroundColor Green
    }
}

Write-Host ""

if ($missingCritical.Count -gt 0) {
    Write-Host "WARNING: Missing critical environment variables!" -ForegroundColor Red
    Write-Host "Please set the following in your .env file:" -ForegroundColor Yellow
    foreach ($var in $missingCritical) {
        Write-Host "  - $var" -ForegroundColor Yellow
    }
    Write-Host ""
    exit 1
}

Write-Host "All critical variables are set!" -ForegroundColor Green
Write-Host ""
Write-Host "You can now run:" -ForegroundColor Cyan
Write-Host "  .\gradlew.bat bootRun" -ForegroundColor White
Write-Host ""
