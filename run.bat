@echo off
chcp 65001 > nul
title Bookora - Java Bookstore Server (MySQL)

echo =======================================================================
echo          📚 HỆ THỐNG WEB BÁN SÁCH JAVA - BOOKORA (MYSQL) 📚
echo =======================================================================
echo.
echo [1/3] Đang kiểm tra và biên dịch mã nguồn Java...

if not exist "build\classes" mkdir "build\classes"

javac -encoding UTF-8 -cp "lib\servlet-api.jar;lib\mysql-connector-j.jar;src\main\java" -d "build\classes" src\main\java\com\bookstore\model\*.java src\main\java\com\bookstore\service\*.java src\main\java\com\bookstore\data\*.java src\main\java\com\bookstore\servlet\*.java src\main\java\com\bookstore\server\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [LỖI] Không thể biên dịch mã Java. Vui lòng kiểm tra lại JDK trên máy.
    pause
    exit /b 1
)

echo [2/3] Biên dịch thành công! Đang khởi động Server...
echo [3/3] Đang mở trình duyệt web tới Trang chủ Bookora...
start "" "http://localhost:8080/home"

echo.
echo =======================================================================
echo Máy chủ đang hoạt động tại cổng 8080 (Nhấn Ctrl + C để dừng)
echo Cơ sở dữ liệu: MySQL 127.0.0.1:3306 (web_bookora)
echo =======================================================================
java -cp "build\classes;lib\servlet-api.jar;lib\mysql-connector-j.jar" com.bookstore.server.BookstoreApp

pause
