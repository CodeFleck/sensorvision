@echo off
echo ========================================
echo SensorVision Backend Restart Script
echo ========================================
echo.

echo [1/3] Checking for processes on port 8080...
netstat -ano | findstr :8080 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    echo Found processes on port 8080. Killing them...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
        echo Killing PID %%a...
        taskkill /F /PID %%a 2>nul
    )
    echo Waiting for port to be released...
    timeout /t 3 /nobreak >nul
) else (
    echo Port 8080 is free.
)

echo.
echo [2/3] Cleaning Gradle build cache...
call gradlew clean --quiet

echo.
echo [3/3] Starting Spring Boot backend...
echo Press Ctrl+C to stop the server
echo ========================================
echo.
call gradlew bootRun
