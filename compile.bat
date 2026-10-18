@echo off
chcp 65001 >nul
cd /d "%~dp0"
title Bookora - Bien dich Backend

echo =====================================================================
echo                    BOOKORA - BIEN DICH BACKEND
echo =====================================================================
echo.

set BUILD_DIR=backend\build\classes
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

echo [1/2] Dang bien dich Java source...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compile.ps1"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [LOI] Bien dich that bai! Kiem tra lai loi o tren.
    pause
    exit /b 1
)

echo.
echo [2/2] BUILD THANH CONG!
echo       Class files: %BUILD_DIR%
echo.
echo =====================================================================
