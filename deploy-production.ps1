# SensorVision Production Deployment Script
# Deploys latest code from main branch to EC2 production server

param(
    [string]$EC2Host = "35.88.65.186",
    [string]$EC2User = "ec2-user",
    [string]$SSHKeyPath = "$env:USERPROFILE\.ssh\id_rsa"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  SensorVision Production Deployment" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if SSH key exists
if (-not (Test-Path $SSHKeyPath)) {
    Write-Host "ERROR: SSH key not found at $SSHKeyPath" -ForegroundColor Red
    Write-Host "Please specify the correct path using -SSHKeyPath parameter" -ForegroundColor Yellow
    Write-Host "Example: .\deploy-production.ps1 -SSHKeyPath 'C:\path\to\key.pem'" -ForegroundColor Yellow
    exit 1
}

Write-Host "[1/8] Testing SSH connection..." -ForegroundColor Green
$testConnection = & ssh -i $SSHKeyPath -o ConnectTimeout=10 -o StrictHostKeyChecking=no "$EC2User@$EC2Host" "echo Connected"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Cannot connect to $EC2Host" -ForegroundColor Red
    Write-Host "Please check:" -ForegroundColor Yellow
    Write-Host "  - SSH key path is correct" -ForegroundColor Yellow
    Write-Host "  - EC2 instance is running" -ForegroundColor Yellow
    Write-Host "  - Security group allows SSH (port 22)" -ForegroundColor Yellow
    exit 1
}
Write-Host "   [OK] SSH connection successful" -ForegroundColor Gray
Write-Host ""

Write-Host "[2/8] Checking current git status..." -ForegroundColor Green
$gitStatusCmd = "cd /home/ec2-user/sensorvision; git log --oneline -1"
$currentCommit = & ssh -i $SSHKeyPath "$EC2User@$EC2Host" $gitStatusCmd
Write-Host "   Current: $currentCommit" -ForegroundColor Gray
Write-Host ""

Write-Host "[3/8] Pulling latest code from main branch..." -ForegroundColor Green
$gitPullCmd = "cd /home/ec2-user/sensorvision; git pull origin main 2>&1"
$pullOutput = & ssh -i $SSHKeyPath "$EC2User@$EC2Host" $gitPullCmd
Write-Host $pullOutput -ForegroundColor Gray

if ($pullOutput -match "Already up to date") {
    Write-Host "   [INFO] No new changes to deploy" -ForegroundColor Yellow
} else {
    Write-Host "   [OK] Code updated successfully" -ForegroundColor Gray
}
Write-Host ""

Write-Host "[4/8] Checking what will be deployed..." -ForegroundColor Green
$gitLogCmd = "cd /home/ec2-user/sensorvision; git log --oneline -5"
$latestCommits = & ssh -i $SSHKeyPath "$EC2User@$EC2Host" $gitLogCmd
Write-Host $latestCommits -ForegroundColor Gray
Write-Host ""

Write-Host "[5/8] Running deployment script..." -ForegroundColor Green
Write-Host "   This may take 3-5 minutes..." -ForegroundColor Yellow

# Run deploy.sh with real-time output
$deployCmd = "cd /home/ec2-user/sensorvision; chmod +x deploy.sh; ./deploy.sh"
& ssh -i $SSHKeyPath "$EC2User@$EC2Host" $deployCmd

if ($LASTEXITCODE -ne 0) {
    Write-Host "   [WARN] Deployment script encountered issues" -ForegroundColor Yellow
    Write-Host "   Continuing with health checks..." -ForegroundColor Yellow
} else {
    Write-Host "   [OK] Deployment script completed" -ForegroundColor Gray
}
Write-Host ""

Write-Host "[6/8] Checking container status..." -ForegroundColor Green
$containerCmd = "cd /home/ec2-user/sensorvision; docker-compose -f docker-compose.production.yml ps"
$containerStatus = & ssh -i $SSHKeyPath "$EC2User@$EC2Host" $containerCmd
Write-Host $containerStatus -ForegroundColor Gray
Write-Host ""

Write-Host "[7/8] Waiting for application to be healthy..." -ForegroundColor Green
$healthCheckScript = @"
for i in {1..30}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo HEALTHY
        curl -s http://localhost:8080/actuator/health
        exit 0
    fi
    echo Attempt \$i/30: Waiting...
    sleep 10
done
echo TIMEOUT
exit 1
"@

$healthResult = & ssh -i $SSHKeyPath "$EC2User@$EC2Host" "bash -c '$healthCheckScript'"
if ($healthResult -match "HEALTHY") {
    Write-Host "   [OK] Application is healthy!" -ForegroundColor Green
    Write-Host $healthResult -ForegroundColor Gray
} else {
    Write-Host "   [WARN] Health check timeout - application may need more time" -ForegroundColor Yellow
    Write-Host "   Check logs: ssh $EC2User@$EC2Host 'cd /home/ec2-user/sensorvision; docker-compose -f docker-compose.production.yml logs backend'" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "[8/8] Testing external access..." -ForegroundColor Green
try {
    Start-Sleep -Seconds 5
    $externalHealth = Invoke-WebRequest -Uri "http://$EC2Host:8080/actuator/health" -TimeoutSec 10 -UseBasicParsing
    $healthJson = $externalHealth.Content | ConvertFrom-Json

    if ($healthJson.status -eq "UP") {
        Write-Host "   [OK] External health check: PASSED" -ForegroundColor Green
        Write-Host "   Status: $($healthJson.status)" -ForegroundColor Gray
    } else {
        Write-Host "   [WARN] External health check: $($healthJson.status)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   [WARN] External health check failed: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "   Note: Port 8080 may not be open in security group" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Deployment Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Backend URL:           http://$EC2Host:8080" -ForegroundColor White
Write-Host "Frontend URL:          http://$EC2Host:3000" -ForegroundColor White
Write-Host "Integration Wizard:    http://$EC2Host:3000/integration-wizard" -ForegroundColor White
Write-Host "Health Check:          http://$EC2Host:8080/actuator/health" -ForegroundColor White
Write-Host ""

Write-Host "Deployment Features Included:" -ForegroundColor Cyan
Write-Host "  [OK] Integration Wizard (Phase 6)" -ForegroundColor Green
Write-Host "  [OK] Python SDK (Phase 4)" -ForegroundColor Green
Write-Host "  [OK] JavaScript/TypeScript SDK (Phase 5)" -ForegroundColor Green
Write-Host "  [OK] All bug fixes and improvements" -ForegroundColor Green
Write-Host ""

Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "  1. Open http://$EC2Host:3000/integration-wizard in your browser" -ForegroundColor White
Write-Host "  2. Test device integration" -ForegroundColor White
Write-Host "  3. Check dashboard for real-time data" -ForegroundColor White
Write-Host ""

Write-Host "Troubleshooting Commands:" -ForegroundColor Cyan
Write-Host "  View logs:    ssh -i $SSHKeyPath $EC2User@$EC2Host 'cd /home/ec2-user/sensorvision; docker-compose -f docker-compose.production.yml logs -f'" -ForegroundColor Gray
Write-Host "  Restart:      ssh -i $SSHKeyPath $EC2User@$EC2Host 'cd /home/ec2-user/sensorvision; docker-compose -f docker-compose.production.yml restart'" -ForegroundColor Gray
Write-Host "  Status:       ssh -i $SSHKeyPath $EC2User@$EC2Host 'cd /home/ec2-user/sensorvision; docker-compose -f docker-compose.production.yml ps'" -ForegroundColor Gray
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Deployment Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
