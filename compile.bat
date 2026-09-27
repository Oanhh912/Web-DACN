@echo off
cd /d "%~dp0"

echo ===================================================
echo [1/2] Dang bien dich ma nguon Java Bookora...
echo ===================================================

if not exist "build\classes" mkdir "build\classes"

javac -encoding UTF-8 -cp "lib\servlet-api.jar;lib\mysql-connector-j.jar;src\main\java" -d "build\classes" src\main\java\com\bookstore\model\*.java src\main\java\com\bookstore\service\*.java src\main\java\com\bookstore\data\*.java src\main\java\com\bookstore\servlet\*.java src\main\java\com\bookstore\server\*.java

if %ERRORLEVEL% EQU 0 (
    echo [THANH CONG] Bien dich hoan tat vao thu muc build\classes!
) else (
    echo [LOI] Qua trinh bien dich gap su co. Vui long kiem tra lai JDK tren may.
)
