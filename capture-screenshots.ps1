# PowerShell Script to Capture Screenshots for SensorVision Documentation
# This script automates the process of capturing screenshots for the integration guide

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

# Configuration
$baseUrl = "http://localhost:3001"
$outputDir = "C:\sensorvision\docs\images"
$browserPath = "C:\Program Files\Google\Chrome\Application\chrome.app"

# Alternative browser paths to try
$browserPaths = @(
    "C:\Program Files\Google\Chrome\Application\chrome.exe",
    "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
    "C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
)

# Find available browser
$browser = $null
foreach ($path in $browserPaths) {
    if (Test-Path $path) {
        $browser = $path
        Write-Host "Found browser: $browser" -ForegroundColor Green
        break
    }
}

if (-not $browser) {
    Write-Host "No browser found. Please install Chrome or Edge." -ForegroundColor Red
    exit 1
}

# Ensure output directory exists
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

# Function to take a screenshot of the primary screen
function Capture-Screen {
    param(
        [string]$FilePath
    )

    try {
        $bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
        $bitmap = New-Object System.Drawing.Bitmap $bounds.Width, $bounds.Height
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        $graphics.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
        $bitmap.Save($FilePath, [System.Drawing.Imaging.ImageFormat]::Png)
        $graphics.Dispose()
        $bitmap.Dispose()
        Write-Host "Screenshot saved: $FilePath" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Host "Error capturing screenshot: $_" -ForegroundColor Red
        return $false
    }
}

# Function to open URL and capture screenshot
function Capture-Page {
    param(
        [string]$Url,
        [string]$FileName,
        [int]$WaitSeconds = 3
    )

    Write-Host "`nCapturing: $FileName" -ForegroundColor Cyan
    Write-Host "URL: $Url" -ForegroundColor Gray

    # Open URL in browser (new window)
    Start-Process $browser -ArgumentList "--new-window", $Url

    # Wait for page to load
    Write-Host "Waiting $WaitSeconds seconds for page to load..." -ForegroundColor Yellow
    Start-Sleep -Seconds $WaitSeconds

    # Maximize window (send F11 key for fullscreen, then F11 again to exit, or use Win+Up)
    # This is a simple approach - maximize the window
    $wshell = New-Object -ComObject wscript.shell
    $wshell.AppActivate($browser)
    Start-Sleep -Milliseconds 500

    # Take screenshot
    $filePath = Join-Path $outputDir $FileName
    $success = Capture-Screen -FilePath $filePath

    if ($success) {
        Write-Host "✓ Captured successfully" -ForegroundColor Green
    }

    # Small delay between captures
    Start-Sleep -Seconds 1

    return $success
}

# Main script execution
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SensorVision Screenshot Capture Tool" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if application is running
Write-Host "Checking if application is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl" -UseBasicParsing -TimeoutSec 5
    Write-Host "✓ Application is running" -ForegroundColor Green
}
catch {
    Write-Host "✗ Application is not running at $baseUrl" -ForegroundColor Red
    Write-Host "Please start the application first with:" -ForegroundColor Yellow
    Write-Host "  Backend:  ./gradlew bootRun" -ForegroundColor Gray
    Write-Host "  Frontend: cd frontend && npm run dev" -ForegroundColor Gray
    exit 1
}

Write-Host ""
Write-Host "IMPORTANT: This script will open multiple browser windows." -ForegroundColor Yellow
Write-Host "Please do not interact with your computer during capture." -ForegroundColor Yellow
Write-Host ""
Write-Host "Press Enter to start capturing screenshots..." -ForegroundColor Cyan
$null = Read-Host

# Screenshot capture sequence
$screenshots = @(
    @{Url="$baseUrl/devices"; File="01-device-management-page.png"; Wait=4; Description="Device Management Page"},
    @{Url="$baseUrl/dashboard"; File="11-dashboard-overview.png"; Wait=5; Description="Dashboard Overview"},
    @{Url="$baseUrl/rules"; File="07-rules-page.png"; Wait=4; Description="Rules Page"},
    @{Url="$baseUrl/alerts"; File="16-alerts-page.png"; Wait=4; Description="Alerts Page"},
    @{Url="$baseUrl/data-ingestion"; File="05-data-ingestion-page.png"; Wait=4; Description="Data Ingestion Page"}
)

$successCount = 0
$totalCount = $screenshots.Count

Write-Host "`nStarting screenshot capture sequence..." -ForegroundColor Cyan
Write-Host "Total screenshots to capture: $totalCount" -ForegroundColor Gray
Write-Host ""

foreach ($screenshot in $screenshots) {
    if (Capture-Page -Url $screenshot.Url -FileName $screenshot.File -WaitSeconds $screenshot.Wait) {
        $successCount++
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Screenshot Capture Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Successfully captured: $successCount / $totalCount" -ForegroundColor Green
Write-Host "Screenshots saved to: $outputDir" -ForegroundColor Gray
Write-Host ""
Write-Host "Note: Some screenshots require manual interaction:" -ForegroundColor Yellow
Write-Host "  - Modal dialogs (Add Device, Create Rule)" -ForegroundColor Gray
Write-Host "  - Form inputs with sample data" -ForegroundColor Gray
Write-Host "  - Success messages" -ForegroundColor Gray
Write-Host ""
Write-Host "Please review SCREENSHOT_GUIDE.md for detailed instructions" -ForegroundColor Cyan
Write-Host "on capturing the remaining interactive screenshots." -ForegroundColor Cyan
Write-Host ""
