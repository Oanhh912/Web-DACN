# Script biên dịch Backend cho Bookora
$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$backendSrc = Join-Path $scriptDir "backend\src\main\java"
$backendLib = Join-Path $scriptDir "backend\lib"
$buildDir = Join-Path $scriptDir "backend\build\classes"

if (-not (Test-Path $buildDir)) {
    New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
}

# Javac @argument_file: đường dẫn có khoảng trắng cần bọc ngoặc kép và dùng forward slash để tránh escape
$javaFiles = Get-ChildItem -Path $backendSrc -Recurse -Filter "*.java" | ForEach-Object {
    '"' + $_.FullName.Replace('\', '/') + '"'
}

$sourcesFile = Join-Path $scriptDir "backend\build\sources.txt"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllLines($sourcesFile, $javaFiles, $utf8NoBom)

$cp = "$backendLib\servlet-api.jar;$backendLib\mysql-connector-j.jar"
& javac -encoding UTF-8 -cp $cp -d $buildDir "@$sourcesFile"
$exitCode = $LASTEXITCODE

if (Test-Path $sourcesFile) {
    Remove-Item -Force $sourcesFile
}

exit $exitCode
