$ErrorActionPreference = "Continue"
$srcRoot = "e:\Web DACN\backend\src\main\java"
$outDir  = "e:\Web DACN\backend\build\classes"
$cp      = "e:\Web DACN\backend\lib\mysql-connector-j.jar;e:\Web DACN\backend\lib\servlet-api.jar;$outDir"

# Compile DataStore first (dependency)
$dataStoreFile = "$srcRoot\com\bookstore\data\DataStore.java"
Write-Host "Compiling DataStore..."
& javac -encoding UTF-8 -cp $cp -d $outDir $dataStoreFile 2>&1
Write-Host "DataStore exit: $LASTEXITCODE"

# Compile BookstoreApp
$appFile = "$srcRoot\com\bookstore\server\BookstoreApp.java"
Write-Host "Compiling BookstoreApp..."
& javac -encoding UTF-8 -cp $cp -d $outDir $appFile 2>&1
Write-Host "BookstoreApp exit: $LASTEXITCODE"
