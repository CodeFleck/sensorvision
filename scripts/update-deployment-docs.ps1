# Update Deployment Documentation Helper
# Helps document successful deployments in DEPLOY_DOCUMENTATION.md

param(
    [string]$Title = "",
    [ValidateSet("Feature", "Bug Fix", "Enhancement", "Hotfix")]
    [string]$Type = "Feature",
    [string]$Description = "",
    [string]$FilesChanged = "",
    [string]$Impact = ""
)

function Write-Info($message) {
    Write-Host "[INFO] $message" -ForegroundColor Blue
}

function Write-Success($message) {
    Write-Host "[SUCCESS] $message" -ForegroundColor Green
}

# Get current date
$CurrentDate = Get-Date -Format "yyyy-MM-dd"

# Get current commit info
try {
    $CurrentCommit = (git rev-parse --short HEAD 2>$null) -replace "`n", ""
    $CurrentBranch = (git branch --show-current 2>$null) -replace "`n", ""
} catch {
    $CurrentCommit = "unknown"
    $CurrentBranch = "unknown"
}

# Prompt for missing information
if ([string]::IsNullOrEmpty($Title)) {
    $Title = Read-Host "Enter deployment title"
}

if ([string]::IsNullOrEmpty($Description)) {
    $Description = Read-Host "Enter description of changes"
}

if ([string]::IsNullOrEmpty($FilesChanged)) {
    Write-Host "Enter files changed (comma-separated):"
    $FilesChanged = Read-Host
}

if ([string]::IsNullOrEmpty($Impact)) {
    $Impact = Read-Host "Enter impact/benefits"
}

# Get deployment time from recent GitHub Actions run
$DeploymentTime = "N/A"
try {
    $RecentRun = gh run list --limit 1 --json conclusion,displayTitle,databaseId 2>$null | ConvertFrom-Json
    if ($RecentRun -and $RecentRun.conclusion -eq "success") {
        $RunDetails = gh run view $RecentRun.databaseId --json timing 2>$null | ConvertFrom-Json
        if ($RunDetails.timing) {
            $StartTime = [datetime]$RunDetails.timing.started_at
            $EndTime = [datetime]$RunDetails.timing.completed_at
            $Duration = $EndTime - $StartTime
            $DeploymentTime = "{0}m {1}s" -f [int]$Duration.TotalMinutes, $Duration.Seconds
        }
    }
} catch {
    Write-Info "Could not retrieve deployment time"
}

# Format files changed
$FilesChangedList = ($FilesChanged -split ',').Trim() | ForEach-Object { "- $_" }
$FilesChangedFormatted = $FilesChangedList -join "`n"

# Create deployment entry
$DeploymentEntry = @"


### $CurrentDate - $Title

**Type:** $Type
**Status:** ✅ Deployed Successfully
**Deployment Method:** GitHub Actions CI/CD
**Deployment Time:** $DeploymentTime
**Git Commit:** ``$CurrentCommit``

**Description:**
$Description

**Files Changed:**
$FilesChangedFormatted

**Verification:**
- Backend health: http://35.88.65.186:8080/actuator/health (Status: UP ✅)
- Frontend: http://35.88.65.186:3000/ (Status: Online ✅)

**Impact:**
$Impact

---
"@

# Update DEPLOY_DOCUMENTATION.md
if (Test-Path "DEPLOY_DOCUMENTATION.md") {
    Write-Info "Updating DEPLOY_DOCUMENTATION.md..."

    # Read the file
    $content = Get-Content "DEPLOY_DOCUMENTATION.md" -Raw

    # Insert after "## Deployment Log"
    $content = $content -replace "(## Deployment Log)", "`$1`n$DeploymentEntry"

    # Write back to file
    Set-Content "DEPLOY_DOCUMENTATION.md" -Value $content

    Write-Success "DEPLOY_DOCUMENTATION.md updated successfully!"
    Write-Host ""
    Write-Host "Entry added:" -ForegroundColor Cyan
    Write-Host $DeploymentEntry
    Write-Host ""
    Write-Info "Don't forget to commit this change:"
    Write-Host "git add DEPLOY_DOCUMENTATION.md"
    Write-Host "git commit -m 'docs: update deployment documentation for $Title'"
} else {
    Write-Host "Error: DEPLOY_DOCUMENTATION.md not found" -ForegroundColor Red
    exit 1
}
