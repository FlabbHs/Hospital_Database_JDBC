# Hospital Management System - Build and Run Script

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Hospital Management System - Build Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Create bin directory if it doesn't exist
if (-not (Test-Path -Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

# Compile Java files
Write-Host "[1/2] Compiling Java files..." -ForegroundColor Yellow
$compileOutput = javac -cp "lib\mysql-connector-j-9.5.0.jar;src" -d bin src\*.java 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Compilation failed!" -ForegroundColor Red
    Write-Host $compileOutput -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "[2/2] Compilation successful!" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting Hospital Management System..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Run the application
java -cp "lib\mysql-connector-j-9.5.0.jar;bin" Main

Write-Host ""
Write-Host "Application closed." -ForegroundColor Yellow
Read-Host "Press Enter to exit"

