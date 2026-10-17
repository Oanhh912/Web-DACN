@echo off
chcp 65001 >nul
cd /d "%~dp0"
title Bookora - Bien dich Backend

echo =====================================================================
echo                    BOOKORA - BIEN DICH BACKEND
echo =====================================================================
echo.

set BACKEND_SRC=backend\src\main\java
set BACKEND_LIB=backend\lib
set BUILD_DIR=backend\build\classes

if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

echo [1/2] Dang bien dich Java source (21 files)...
echo       Nguon : %BACKEND_SRC%
echo       Output: %BUILD_DIR%
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$files = Get-ChildItem -Path '%BACKEND_SRC%' -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName; ^
   $args = @('-encoding','UTF-8','-cp','%BACKEND_LIB%\servlet-api.jar;%BACKEND_LIB%\mysql-connector-j.jar','-d','%BUILD_DIR%') + $files; ^
   & javac @args; exit $LASTEXITCODE"

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
