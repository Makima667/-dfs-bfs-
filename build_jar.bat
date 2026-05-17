@echo off
setlocal EnableExtensions

set "PROJECT_DIR=%~dp0"
set "JAVAFX_HOME=C:\Users\q1763\Desktop\javafx-sdk-24.0.2"
set "JAVAFX_LIB=%JAVAFX_HOME%\lib"
set "SRC_DIR=%PROJECT_DIR%java"
set "OUT_DIR=%PROJECT_DIR%out"
set "DIST_DIR=%PROJECT_DIR%dist"
set "SOURCE_LIST=%OUT_DIR%\sources.txt"

if not exist "%JAVAFX_LIB%\javafx.controls.jar" (
    echo [ERROR] JavaFX was not found: %JAVAFX_LIB%
    if /I not "%~1"=="--nopause" pause
    exit /b 1
)

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] javac was not found. Please install JDK and add it to PATH.
    if /I not "%~1"=="--nopause" pause
    exit /b 1
)

where jar >nul 2>nul
if errorlevel 1 (
    echo [ERROR] jar was not found. Please install JDK and add it to PATH.
    if /I not "%~1"=="--nopause" pause
    exit /b 1
)

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
dir /b /s "%SRC_DIR%\*.java" > "%SOURCE_LIST%"

echo [1/3] Compiling...
javac --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml,javafx.swing -encoding UTF-8 -d "%OUT_DIR%" @"%SOURCE_LIST%"
if errorlevel 1 (
    echo [ERROR] Compile failed.
    if /I not "%~1"=="--nopause" pause
    exit /b 1
)

copy /Y "%SRC_DIR%\UI.fxml" "%OUT_DIR%\UI.fxml" >nul

echo [2/3] Creating manifest...
echo Main-Class: Main> "%OUT_DIR%\MANIFEST.MF"
echo.>> "%OUT_DIR%\MANIFEST.MF"

echo [3/3] Building JAR...
jar cfm "%DIST_DIR%\PathLab.jar" "%OUT_DIR%\MANIFEST.MF" -C "%OUT_DIR%" .
if errorlevel 1 (
    echo [ERROR] JAR build failed.
    if /I not "%~1"=="--nopause" pause
    exit /b 1
)

echo.
echo Done: %DIST_DIR%\PathLab.jar
echo You still need JavaFX module path when running this JAR.
if /I not "%~1"=="--nopause" pause
