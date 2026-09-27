@echo off
cd /d "%~dp0"
title Bookora - Java Bookstore Server

echo =======================================================================
echo               HE THONG WEB BAN SACH JAVA - BOOKORA
echo =======================================================================
echo.
echo [1/3] Dang kiem tra va bien dich ma nguon Java...

if not exist "build\classes" mkdir "build\classes"

javac -encoding UTF-8 -cp "lib\servlet-api.jar;lib\mysql-connector-j.jar;src\main\java" -d "build\classes" src\main\java\com\bookstore\model\*.java src\main\java\com\bookstore\service\*.java src\main\java\com\bookstore\data\*.java src\main\java\com\bookstore\servlet\*.java src\main\java\com\bookstore\server\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Khong the bien dich ma Java. Vui long kiem tra lai JDK tren may.
    pause
    exit /b 1
)

echo [2/3] Bien dich thanh cong! Dang khoi dong Server...
echo [3/3] Dang mo trinh duyet web toi Trang chu Bookora...
start "" "http://localhost:8080/home"

echo.
echo =======================================================================
echo May chu dang hoat dong tai cong 8080 (Nhan Ctrl + C de dung)
echo Dia chi: http://localhost:8080/home hoac http://localhost:8080/login
echo =======================================================================
java -cp "build\classes;lib\servlet-api.jar;lib\mysql-connector-j.jar" com.bookstore.server.BookstoreApp

pause
