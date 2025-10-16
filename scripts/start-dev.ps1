# Start Development Environment (Windows PowerShell)
# This script stops existing containers, kills port 8080 processes, loads env vars, and starts services

param(
    [switch]$SkipPortCheck,
    [switch]$BuildImages
)

$ErrorActionPreference = "Continue"

function Write-ColorOutput {
    param([string]$Message, [string]$Color = "White")
    Write-Host $Message -ForegroundColor $Color
}

Write-ColorOutput "================================" "Cyan"
Write-ColorOutput "Starting Development Environment" "Cyan"
Write-ColorOutput "================================" "Cyan"
Write-Host ""

# Step 1: Stop existing containers
Write-ColorOutput "Step 1: Stopping existing containers..." "Yellow"
docker-compose down 2>$null
if ($?) {
    Write-ColorOutput "  OK Containers stopped" "Green"
} else {
    Write-ColorOutput "  OK No containers to stop" "Gray"
}
Write-Host ""

# Step 2: Kill processes on port 8080
if (-not $SkipPortCheck) {
    Write-ColorOutput "Step 2: Checking port 8080..." "Yellow"

    $port8080 = netstat -ano | Select-String ":8080" | Select-String "LISTENING"

    if ($port8080) {
        Write-ColorOutput "  WARNING Port 8080 is in use" "Yellow"

        $pids = @()
        $port8080 | ForEach-Object {
            if ($_ -match '\s+(\d+)\s*$') {
                $pids += $matches[1]
            }
        }

        $pids | Select-Object -Unique | ForEach-Object {
            $pid = $_
            Write-ColorOutput "    Killing process $pid..." "Yellow"
            taskkill //F //PID $pid 2>$null | Out-Null
            if ($?) {
                Write-ColorOutput "    OK Process $pid terminated" "Green"
            } else {
                Write-ColorOutput "    WARNING Could not kill process $pid" "Red"
            }
        }

        Start-Sleep -Seconds 2

        $stillInUse = netstat -ano | Select-String ":8080" | Select-String "LISTENING"
        if ($stillInUse) {
            Write-ColorOutput "  ERROR Port 8080 is still in use" "Red"
            exit 1
        }
    }

    Write-ColorOutput "  OK Port 8080 is available" "Green"
} else {
    Write-ColorOutput "Step 2: Skipping port check" "Gray"
}
Write-Host ""

# Step 3: Load environment variables
Write-ColorOutput "Step 3: Loading environment variables..." "Yellow"

$envFile = Join-Path $PSScriptRoot "..\.env"

if (-not (Test-Path $envFile)) {
    Write-ColorOutput "  ERROR .env file not found!" "Red"
    Write-ColorOutput "  Copy .env.example to .env and configure your settings" "Yellow"
    exit 1
}

$loadedVars = 0
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if (-not [string]::IsNullOrWhiteSpace($line) -and -not $line.StartsWith("#")) {
        if ($line -match '^([^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim().Trim('"').Trim("'")
            [System.Environment]::SetEnvironmentVariable($key, $value, [System.EnvironmentVariableTarget]::Process)
            $loadedVars++
        }
    }
}

Write-ColorOutput "  OK Loaded $loadedVars environment variables" "Green"
Write-Host ""

# Step 4: Start Docker Compose
Write-ColorOutput "Step 4: Starting Docker services..." "Yellow"

if ($BuildImages) {
    Write-ColorOutput "  Building images..." "Cyan"
    docker-compose up -d --build
} else {
    docker-compose up -d
}

if ($?) {
    Write-Host ""
    Write-ColorOutput "  OK Services started!" "Green"
    Write-Host ""

    Write-ColorOutput "Step 5: Checking service health..." "Yellow"
    Start-Sleep -Seconds 3

    docker-compose ps

    Write-Host ""
    Write-ColorOutput "================================" "Green"
    Write-ColorOutput "Development Environment Ready!" "Green"
    Write-ColorOutput "================================" "Green"
    Write-Host ""

    Write-ColorOutput "Services available at:" "Cyan"
    Write-Host "  Backend API:  http://localhost:8080" -ForegroundColor White
    Write-Host "  Frontend:     http://localhost:3001" -ForegroundColor White
    Write-Host "  API Docs:     http://localhost:8080/swagger-ui.html" -ForegroundColor White
    Write-Host "  Health Check: http://localhost:8080/actuator/health" -ForegroundColor White
    Write-Host ""

    Write-ColorOutput "Useful commands:" "Cyan"
    Write-Host "  View logs:      docker-compose logs -f" -ForegroundColor Gray
    Write-Host "  View backend:   docker-compose logs -f backend" -ForegroundColor Gray
    Write-Host "  Stop services:  docker-compose down" -ForegroundColor Gray
    Write-Host "  Restart:        .\scripts\start-dev.ps1" -ForegroundColor Gray
    Write-Host ""

} else {
    Write-ColorOutput "  ERROR Failed to start services!" "Red"
    Write-Host ""
    Write-ColorOutput "Troubleshooting:" "Yellow"
    Write-Host "  1. Check Docker is running: docker ps" -ForegroundColor Gray
    Write-Host "  2. View logs: docker-compose logs" -ForegroundColor Gray
    Write-Host "  3. Check .env file exists and is configured" -ForegroundColor Gray
    Write-Host ""
    exit 1
}
