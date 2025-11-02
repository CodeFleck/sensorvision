@echo off
REM SensorVision Comprehensive Regression Test Suite (Windows)
REM
REM This script orchestrates all regression tests on Windows
REM
REM Usage:
REM   run-regression-tests.bat            # Run all tests
REM   run-regression-tests.bat backend    # Backend only
REM   run-regression-tests.bat frontend   # Frontend only
REM   run-regression-tests.bat flows      # Integration flows only
REM   run-regression-tests.bat health     # Health check only

setlocal enabledelayedexpansion

set MODE=%1
if "%MODE%"=="" set MODE=all

REM Test results
set BACKEND_RESULT=0
set FRONTEND_RESULT=0
set FLOWS_RESULT=0
set HEALTH_RESULT=0

echo.
echo ===============================================================
echo.
echo      🧪  SensorVision Regression Test Suite  🧪
echo.
echo      The Most Comprehensive IoT Testing System
echo.
echo ===============================================================
echo.

REM Check if backend is running
echo Checking if Backend is running...
curl -sf http://localhost:8080/actuator/health >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Backend is not running at http://localhost:8080
    echo Please start backend with: gradlew bootRun
    exit /b 1
) else (
    echo [OK] Backend is running
)

REM Check if frontend is running
echo Checking if Frontend is running...
curl -sf http://localhost:3001 >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Frontend is not running at http://localhost:3001
    echo Please start frontend with: cd frontend ^&^& npm run dev
    exit /b 1
) else (
    echo [OK] Frontend is running
)

echo.

if "%MODE%"=="backend" goto BACKEND_TESTS
if "%MODE%"=="frontend" goto FRONTEND_TESTS
if "%MODE%"=="flows" goto FLOW_TESTS
if "%MODE%"=="health" goto HEALTH_CHECK
goto ALL_TESTS

:BACKEND_TESTS
echo ===============================================================
echo   1. Backend Integration Tests
echo ===============================================================
echo.
echo Running Java/Spring Boot tests...
echo.

call gradlew clean test --tests "org.sensorvision.regression.*" --info
if errorlevel 1 (
    echo.
    echo [FAILED] Backend tests FAILED
    set BACKEND_RESULT=1
) else (
    echo.
    echo [PASSED] Backend tests PASSED
    set BACKEND_RESULT=0
)

if not "%MODE%"=="all" goto END
goto FRONTEND_TESTS

:FRONTEND_TESTS
echo.
echo ===============================================================
echo   2. Frontend E2E Tests with Visual Regression
echo ===============================================================
echo.
echo Installing Playwright browsers...
cd frontend
call npx playwright install chromium --with-deps >nul 2>&1

echo Running Playwright E2E tests...
echo.

call npm run test:e2e -- --project=chromium
if errorlevel 1 (
    echo.
    echo [FAILED] Frontend E2E tests FAILED
    echo Screenshots available in: frontend\test-results\
    set FRONTEND_RESULT=1
) else (
    echo.
    echo [PASSED] Frontend E2E tests PASSED
    set FRONTEND_RESULT=0
)

cd ..

if not "%MODE%"=="all" goto END
goto FLOW_TESTS

:FLOW_TESTS
echo.
echo ===============================================================
echo   3. Integration Flow Tests (MQTT → WebSocket)
echo ===============================================================
echo.
echo Installing flow test dependencies...
cd regression-tests\flows
call npm install >nul 2>&1

echo Running MQTT to WebSocket flow test...
echo.

call npm test
if errorlevel 1 (
    echo.
    echo [FAILED] Integration flow tests FAILED
    set FLOWS_RESULT=1
) else (
    echo.
    echo [PASSED] Integration flow tests PASSED
    set FLOWS_RESULT=0
)

cd ..\..

if not "%MODE%"=="all" goto END
goto HEALTH_CHECK

:HEALTH_CHECK
echo.
echo ===============================================================
echo   4. System Health Check
echo ===============================================================
echo.
echo Installing health check dependencies...
cd regression-tests\health-check
call npm install >nul 2>&1

echo Running comprehensive health check...
echo.

call npm run check
if errorlevel 1 (
    echo.
    echo [FAILED] Health check FAILED
    echo Check health-report.html for details
    set HEALTH_RESULT=1
) else (
    echo.
    echo [PASSED] Health check PASSED
    echo HTML report: regression-tests\health-check\health-report.html
    set HEALTH_RESULT=0
)

cd ..\..

if not "%MODE%"=="all" goto END
goto SUMMARY

:ALL_TESTS
call :BACKEND_TESTS
call :FRONTEND_TESTS
call :FLOW_TESTS
call :HEALTH_CHECK
goto SUMMARY

:SUMMARY
echo.
echo ===============================================================
echo   Test Suite Summary
echo ===============================================================
echo.
echo Results:
echo.

set /a TOTAL_TESTS=4
set /a PASSED_TESTS=0
set /a FAILED_TESTS=0

if %BACKEND_RESULT%==0 (
    echo   [PASS] Backend Integration Tests
    set /a PASSED_TESTS+=1
) else (
    echo   [FAIL] Backend Integration Tests
    set /a FAILED_TESTS+=1
)

if %FRONTEND_RESULT%==0 (
    echo   [PASS] Frontend E2E Tests
    set /a PASSED_TESTS+=1
) else (
    echo   [FAIL] Frontend E2E Tests
    set /a FAILED_TESTS+=1
)

if %FLOWS_RESULT%==0 (
    echo   [PASS] Integration Flow Tests
    set /a PASSED_TESTS+=1
) else (
    echo   [FAIL] Integration Flow Tests
    set /a FAILED_TESTS+=1
)

if %HEALTH_RESULT%==0 (
    echo   [PASS] System Health Check
    set /a PASSED_TESTS+=1
) else (
    echo   [FAIL] System Health Check
    set /a FAILED_TESTS+=1
)

echo.
echo ===============================================================
echo.

if %FAILED_TESTS%==0 (
    echo     ✅  ALL REGRESSION TESTS PASSED  ✅
    echo.
    echo     System is HEALTHY and ready for deployment
    echo.
    exit /b 0
) else (
    echo     ❌  %FAILED_TESTS%/%TOTAL_TESTS% TEST SUITE^(S^) FAILED  ❌
    echo.
    echo     Please review test results before deployment
    echo.
    echo Test Reports:
    echo   - Backend:   build\reports\tests\test\index.html
    echo   - Frontend:  frontend\test-results\html\index.html
    echo   - Health:    regression-tests\health-check\health-report.html
    echo.
    exit /b 1
)

:END
endlocal
