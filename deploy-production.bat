@echo off
REM SensorVision Production Deployment Script (Batch Version)
REM Deploys latest code from main branch to EC2 production server

setlocal enabledelayedexpansion

set EC2_HOST=35.88.65.186
set EC2_USER=ec2-user
set SSH_KEY=%USERPROFILE%\.ssh\id_rsa

REM Check if SSH key path provided as argument
if not "%~1"=="" set SSH_KEY=%~1

echo ========================================
echo   SensorVision Production Deployment
echo ========================================
echo.

REM Check if SSH key exists
if not exist "%SSH_KEY%" (
    echo ERROR: SSH key not found at %SSH_KEY%
    echo Please specify the correct path as first argument
    echo Example: deploy-production.bat "C:\path\to\key.pem"
    exit /b 1
)

echo [1/8] Testing SSH connection...
ssh -i "%SSH_KEY%" -o ConnectTimeout=10 -o StrictHostKeyChecking=no %EC2_USER%@%EC2_HOST% "echo Connected" >nul 2>&1
if errorlevel 1 (
    echo ERROR: Cannot connect to %EC2_HOST%
    echo Please check:
    echo   - SSH key path is correct
    echo   - EC2 instance is running
    echo   - Security group allows SSH port 22
    exit /b 1
)
echo    Done
echo.

echo [2/8] Checking current git status...
ssh -i "%SSH_KEY%" %EC2_USER%@%EC2_HOST% "cd /home/ec2-user/sensorvision && git log --oneline -1"
echo.

echo [3/8] Pulling latest code from main branch...
ssh -i "%SSH_KEY%" %EC2_USER%@%EC2_HOST% "cd /home/ec2-user/sensorvision && git pull origin main"
echo.

echo [4/8] Latest commits being deployed:
ssh -i "%SSH_KEY%" %EC2_USER%@%EC2_HOST% "cd /home/ec2-user/sensorvision && git log --oneline -5"
echo.

echo [5/8] Running deployment script...
echo    This may take 3-5 minutes...
ssh -i "%SSH_KEY%" %EC2_USER%@%EC2_HOST% "cd /home/ec2-user/sensorvision && chmod +x deploy.sh && ./deploy.sh"
echo.

echo [6/8] Checking container status...
ssh -i "%SSH_KEY%" %EC2_USER%@%EC2_HOST% "cd /home/ec2-user/sensorvision && docker-compose -f docker-compose.production.yml ps"
echo.

echo [7/8] Waiting for application to be healthy...
ssh -i "%SSH_KEY%" %EC2_USER%@%EC2_HOST% "for i in {1..30}; do if curl -f http://localhost:8080/actuator/health ^>^/dev^/null 2^>^&1; then echo HEALTHY; curl -s http://localhost:8080/actuator/health; exit 0; fi; echo Attempt $i/30: Waiting...; sleep 10; done; echo TIMEOUT"
echo.

echo [8/8] Testing external access...
curl -s http://%EC2_HOST%:8080/actuator/health
echo.

echo ========================================
echo   Deployment Complete!
echo ========================================
echo.
echo Backend URL:          http://%EC2_HOST%:8080
echo Frontend URL:         http://%EC2_HOST%:3000
echo Integration Wizard:   http://%EC2_HOST%:3000/integration-wizard
echo Health Check:         http://%EC2_HOST%:8080/actuator/health
echo.
echo Deployment Features Included:
echo   * Integration Wizard Phase 6
echo   * Python SDK Phase 4
echo   * JavaScript/TypeScript SDK Phase 5
echo   * All bug fixes and improvements
echo.
echo Troubleshooting:
echo   View logs:  ssh -i "%SSH_KEY%" %EC2_USER%@%EC2_HOST% "cd /home/ec2-user/sensorvision && docker-compose -f docker-compose.production.yml logs -f"
echo.

endlocal
