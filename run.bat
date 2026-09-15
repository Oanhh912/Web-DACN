@echo off
chcp 65001 > nul
title BookHaven - Java Bookstore Server

echo =======================================================================
echo          📚 HỆ THỐNG WEB BÁN SÁCH JAVA - BOOKHAVEN 📚
echo =======================================================================
echo.
echo [1/3] Đang kiểm tra và biên dịch mã nguồn Java...

if not exist "build\classes" mkdir "build\classes"

javac -encoding UTF-8 -cp "lib\servlet-api.jar;src\main\java" -d "build\classes" src\main\java\com\bookstore\model\*.java src\main\java\com\bookstore\data\*.java src\main\java\com\bookstore\servlet\*.java src\main\java\com\bookstore\server\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [LỖI] Không thể biên dịch mã Java. Vui lòng kiểm tra lại JDK trên máy.
    pause
    exit /b 1
)

echo [2/3] Biên dịch thành công! Đang khởi động Server...
echo [3/3] Đang mở trình duyệt web tới trang đăng nhập...
start "" "http://localhost:8080/login"

echo.
echo =======================================================================
echo Máy chủ đang hoạt động tại cổng 8080 (Nhấn Ctrl + C để dừng)
echo =======================================================================
java -cp "build\classes;lib\servlet-api.jar" com.bookstore.server.BookstoreApp

pause
