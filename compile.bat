@echo off
chcp 65001 > nul
echo ===================================================
echo [1/2] Đang biên dịch mã nguồn Java Bookora (MySQL)...
echo ===================================================

if not exist "build\classes" mkdir "build\classes"

javac -encoding UTF-8 -cp "lib\servlet-api.jar;lib\mysql-connector-j.jar;src\main\java" -d "build\classes" src\main\java\com\bookstore\model\*.java src\main\java\com\bookstore\data\*.java src\main\java\com\bookstore\servlet\*.java src\main\java\com\bookstore\server\*.java

if %ERRORLEVEL% EQU 0 (
    echo [THÀNH CÔNG] Biên dịch hoàn tất vào thư mục build\classes!
) else (
    echo [LỖI] Quá trình biên dịch gặp sự cố. Vui lòng kiểm tra lại.
)
pause
