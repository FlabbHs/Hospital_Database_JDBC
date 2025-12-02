@echo off
REM Compile and Run Hospital Management System

echo ========================================
echo Hospital Management System - Build Script
echo ========================================
echo.

REM Create bin directory if it doesn't exist
if not exist bin mkdir bin

REM Compile Java files
echo [1/2] Compiling Java files...
javac -cp "lib\mysql-connector-j-9.5.0.jar;src" -d bin src\*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Compilation failed!
    echo.
    pause
    exit /b 1
)

REM Copy resources (db.properties) to bin so it's on the classpath
if exist src\db.properties copy /Y src\db.properties bin\ >nul

echo [2/2] Compilation successful!
echo.
echo ========================================
echo Starting Hospital Management System...
echo ========================================
echo.

echo Using classpath: lib\mysql-connector-j-9.5.0.jar;bin
REM Run the application
java -cp "lib\mysql-connector-j-9.5.0.jar;bin" Main


echo.
echo Application closed.
pause
