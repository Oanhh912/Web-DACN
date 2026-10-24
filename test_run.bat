@echo off
chcp 65001 >nul
cd /d "%~dp0"
title Bookora - Java Bookstore Server

echo =======================================================================
echo                HE THONG WEB BAN SACH JAVA - BOOKORA
echo =======================================================================
echo.
echo Cau truc du an:
echo   frontend/  - Giao dien HTML/CSS/JS (phuc vu boi backend)
echo   backend/   - May chu Java (xu ly API, DB, auth, email...)
echo   config/    - Cau hinh moi truong (email, SMS, AI key)
echo.

set BACKEND_SRC=backend\src\main\java
set BACKEND_LIB=backend\lib
set BUILD_DIR=backend\build\classes

if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

echo [1/3] Dang bien dich ma nguon Java...

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compile.ps1"

if %ERRORLEVEL% NEQ 0 (
    echo.