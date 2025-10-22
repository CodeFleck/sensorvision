# Generate placeholder images for screenshots with descriptions
Add-Type -AssemblyName System.Drawing

$outputDir = "C:\sensorvision\docs\images"

# Ensure output directory exists
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

function Create-PlaceholderImage {
    param(
        [string]$FileName,
        [string]$Title,
        [string[]]$Description,
        [int]$Width = 1920,
        [int]$Height = 1080
    )

    $filePath = Join-Path $outputDir $FileName

    # Create bitmap
    $bitmap = New-Object System.Drawing.Bitmap $Width, $Height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)

    # Set high quality
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias

    # Background
    $graphics.Clear([System.Drawing.Color]::FromArgb(249, 250, 251))

    # Draw border
    $borderPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(209, 213, 219), 2)
    $graphics.DrawRectangle($borderPen, 10, 10, $Width - 20, $Height - 20)

    # Title
    $titleFont = New-Object System.Drawing.Font("Segoe UI", 32, [System.Drawing.FontStyle]::Bold)
    $titleBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(17, 24, 39))
    $titleFormat = New-Object System.Drawing.StringFormat
    $titleFormat.Alignment = [System.Drawing.StringAlignment]::Center
    $titleFormat.LineAlignment = [System.Drawing.StringAlignment]::Center

    $titleRect = New-Object System.Drawing.RectangleF(0, 100, $Width, 60)
    $graphics.DrawString($Title, $titleFont, $titleBrush, $titleRect, $titleFormat)

    # Description
    $descFont = New-Object System.Drawing.Font("Segoe UI", 18)
    $descBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(75, 85, 99))
    $descFormat = New-Object System.Drawing.StringFormat
    $descFormat.Alignment = [System.Drawing.StringAlignment]::Center

    $y = 200
    foreach ($line in $Description) {
        $descRect = New-Object System.Drawing.RectangleF(0, $y, $Width, 30)
        $graphics.DrawString($line, $descFont, $descBrush, $descRect, $descFormat)
        $y += 35
    }

    # Watermark
    $watermarkFont = New-Object System.Drawing.Font("Segoe UI", 14)
    $watermarkBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(156, 163, 175))
    $watermarkRect = New-Object System.Drawing.RectangleF(0, $Height - 100, $Width, 30)
    $graphics.DrawString("SensorVision - Screenshot Placeholder", $watermarkFont, $watermarkBrush, $watermarkRect, $titleFormat)
    $graphics.DrawString("Replace with actual screenshot from running application", $watermarkFont, $watermarkBrush, (New-Object System.Drawing.RectangleF(0, $Height - 65, $Width, 30)), $titleFormat)

    # Save
    $bitmap.Save($filePath, [System.Drawing.Imaging.ImageFormat]::Png)

    # Cleanup
    $graphics.Dispose()
    $bitmap.Dispose()
    $titleFont.Dispose()
    $descFont.Dispose()
    $watermarkFont.Dispose()
    $titleBrush.Dispose()
    $descBrush.Dispose()
    $watermarkBrush.Dispose()
    $borderPen.Dispose()

    Write-Host "Created: $FileName" -ForegroundColor Green
}

Write-Host "Generating placeholder images..." -ForegroundColor Cyan
Write-Host ""

# Generate all placeholder images
Create-PlaceholderImage -FileName "01-device-management-page.png" -Title "Device Management Page" -Description @(
    "Shows the main devices page with device list table",
    "Includes 'Add Device' button in top-right",
    "Displays device columns: Name, Status, Location, Type, Last Seen, Actions"
)

Create-PlaceholderImage -FileName "02-add-device-modal.png" -Title "Add Device Modal" -Description @(
    "Modal dialog for adding a new device",
    "Shows empty form fields ready for input",
    "Fields: Device ID, Device Name, Location, Sensor Type, Firmware Version"
) -Width 800 -Height 900

Create-PlaceholderImage -FileName "03-device-modal-filled.png" -Title "Device Modal with Sample Data" -Description @(
    "Same modal with example data filled in",
    "Device ID: sensor-demo-001",
    "Device Name: Smart Meter - Demo Building",
    "Location: Building A - Floor 1 - Room 101"
) -Width 800 -Height 900

Create-PlaceholderImage -FileName "04-device-list-with-new.png" -Title "Device List After Creation" -Description @(
    "Devices page showing newly created device",
    "New device appears in table with UNKNOWN or OFFLINE status",
    "Demonstrates successful device registration"
)

Create-PlaceholderImage -FileName "05-data-ingestion-page.png" -Title "HTTP Data Ingestion Page" -Description @(
    "Two-column layout with ingestion forms",
    "Left: Full Telemetry Ingestion form",
    "Right: Single Variable Ingestion form",
    "API documentation section at bottom"
)

Create-PlaceholderImage -FileName "06-data-ingestion-success.png" -Title "Data Ingestion Success Response" -Description @(
    "Shows successful data submission",
    "Green success message with response JSON",
    "Demonstrates HTTP API in action"
)

Create-PlaceholderImage -FileName "07-rules-page.png" -Title "Rules & Automation Page" -Description @(
    "List of configured monitoring rules",
    "'Create Rule' button in top-right",
    "Table shows: Rule Name, Device, Condition, Status, Created Date, Actions"
)

Create-PlaceholderImage -FileName "08-create-rule-modal.png" -Title "Create Rule Modal" -Description @(
    "Modal dialog for creating a new rule",
    "Empty form showing all configuration fields",
    "Fields: Rule Name, Description, Device, Variable, Operator, Threshold"
) -Width 800 -Height 900

Create-PlaceholderImage -FileName "09-create-rule-filled.png" -Title "Create Rule with Sample Data" -Description @(
    "Rule modal with example configuration",
    "Rule Name: High Power Consumption Alert",
    "Variable: Power Consumption > 100 kW",
    "Enable immediately checkbox selected"
) -Width 800 -Height 900

Create-PlaceholderImage -FileName "10-rules-list-active.png" -Title "Rules List with Active Rules" -Description @(
    "Rules page with created rules visible",
    "Shows enabled/disabled toggle states",
    "Displays rule conditions in table format"
)

Create-PlaceholderImage -FileName "11-dashboard-overview.png" -Title "Dashboard Overview" -Description @(
    "Full dashboard showing all components",
    "Top: Connection status and metric cards",
    "Middle: Real-time power consumption chart",
    "Bottom: Grid of device cards"
)

Create-PlaceholderImage -FileName "12-dashboard-status-cards.png" -Title "Dashboard Status Cards" -Description @(
    "Close-up of three metric cards",
    "Total Devices, Online Devices, Total Power",
    "Shows aggregated statistics"
) -Width 1600 -Height 400

Create-PlaceholderImage -FileName "13-realtime-chart.png" -Title "Real-time Power Consumption Chart" -Description @(
    "Line chart showing live data",
    "Multiple lines for different devices",
    "Connection status showing 'Open'"
) -Width 1600 -Height 600

Create-PlaceholderImage -FileName "14-device-cards-grid.png" -Title "Device Cards Grid" -Description @(
    "Grid layout of individual device cards",
    "Each card shows device status and latest readings",
    "Mix of ONLINE/OFFLINE status badges"
)

Create-PlaceholderImage -FileName "15-device-card-detail.png" -Title "Single Device Card Detail" -Description @(
    "Close-up of one device card",
    "Shows: Name, ID, Status, Location",
    "Latest telemetry values for all variables",
    "Last seen timestamp"
) -Width 600 -Height 400

Create-PlaceholderImage -FileName "16-alerts-page.png" -Title "Alerts Page" -Description @(
    "List of triggered alerts",
    "Shows severity levels (LOW, MEDIUM, HIGH)",
    "Alert details: Device, Rule, Value, Timestamp"
)

Create-PlaceholderImage -FileName "17-websocket-status.png" -Title "WebSocket Connection Status" -Description @(
    "Connection status indicator",
    "Shows 'Open' for active real-time connection",
    "Located at top of dashboard"
) -Width 800 -Height 200

Write-Host ""
Write-Host "All placeholder images generated successfully!" -ForegroundColor Green
Write-Host "Location: $outputDir" -ForegroundColor Gray
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Open http://localhost:3001 in your browser" -ForegroundColor Gray
Write-Host "2. Follow SCREENSHOT_GUIDE.md to capture actual screenshots" -ForegroundColor Gray
Write-Host "3. Replace placeholder images with real screenshots" -ForegroundColor Gray
Write-Host "4. Screenshots are already referenced in SENSOR_INTEGRATION_GUIDE.md" -ForegroundColor Gray
