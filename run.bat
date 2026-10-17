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

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$files = Get-ChildItem -Path '%BACKEND_SRC%' -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName; ^
   $args = @('-encoding','UTF-8','-cp','%BACKEND_LIB%\servlet-api.jar;%BACKEND_LIB%\mysql-connector-j.jar','-d','%BUILD_DIR%') + $files; ^
   & javac @args; exit $LASTEXITCODE"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [LOI] Khong the bien dich ma Java. Kiem tra JDK (can Java 11+).
    pause
    exit /b 1
)

echo [2/3] Bien dich thanh cong! Dang khoi dong Server...
echo [3/3] Dang mo trinh duyet...
start "" "http://localhost:8080/home"

echo.
echo =======================================================================
echo May chu dang hoat dong tai cong 8080 (Nhan Ctrl + C de dung)
echo Trang chu      : http://localhost:8080/home
echo Dang nhap      : http://localhost:8080/login
echo Quan tri vien  : http://localhost:8080/admin
echo.
echo Tai khoan thu nghiem:
echo   admin      / 123456   (Quan Tri Vien)
echo   oanh       / 123456   (Khach Hang)
echo   khachhang  / 123456   (Khach Hang)
echo =======================================================================

java -cp "%BUILD_DIR%;%BACKEND_LIB%\servlet-api.jar;%BACKEND_LIB%\mysql-connector-j.jar" ^
     com.bookstore.server.BookstoreApp

pause
