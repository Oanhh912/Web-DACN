@echo off
powershell -Command "(Get-ChildItem -Path 'src/main/java' -Recurse -Filter '*.java').FullName.Replace('\', '/') | ForEach-Object { '\"' + $_ + '\"' } | Set-Content -Path 'sources.txt'"
javac -encoding UTF-8 -d "target\classes" -cp "target\classes;src\main\webapp\WEB-INF\lib\mysql-connector-j-8.3.0.jar;src\main\webapp\WEB-INF\lib\servlet-api.jar" @sources.txt
if %errorlevel% equ 0 (
    echo BUILD SUCCESS
    del sources.txt
) else (
    echo BUILD FAILED
)
