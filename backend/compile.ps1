$ErrorActionPreference = "Continue"
$srcRoot = "e:\Web DACN\backend\src\main\java"
$outDir  = "e:\Web DACN\backend\build\classes"
$cp      = "e:\Web DACN\backend\lib\mysql-connector-j.jar;e:\Web DACN\backend\lib\servlet-api.jar"

$srcFiles = Get-ChildItem -Recurse $srcRoot -Filter "*.java" | Select-Object -ExpandProperty FullName

& javac -encoding UTF-8 -cp $cp -d $outDir @srcFiles 2>&1
Write-Host "Exit code: $LASTEXITCODE"
