@echo off
setlocal EnableExtensions

set "PROJECT_DIR=%~dp0"
set "JAVAFX_HOME=C:\Users\q1763\Desktop\javafx-sdk-24.0.2"
set "JAVAFX_LIB=%JAVAFX_HOME%\lib"
set "SRC_DIR=%PROJECT_DIR%java"
set "OUT_DIR=%PROJECT_DIR%out"
set "SOURCE_LIST=%OUT_DIR%\sources.txt"

echo Project directory: %PROJECT_DIR%
echo JavaFX lib: %JAVAFX_LIB%
echo.

if not exist "%JAVAFX_LIB%\javafx.controls.jar" (
    echo [ERROR] JavaFX was not found.
    echo Expected: %JAVAFX_LIB%\javafx.controls.jar
    echo Please check that javafx-sdk-24.0.2 is on your Desktop.
    pause
    exit /b 1
)

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] javac was not found.
    echo Please install JDK and add it to PATH.
    pause
    exit /b 1
)

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
dir /b /s "%SRC_DIR%\*.java" > "%SOURCE_LIST%"

echo [1/2] Compiling...
javac --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml,javafx.swing -encoding UTF-8 -d "%OUT_DIR%" @"%SOURCE_LIST%"
if errorlevel 1 (
    echo.
    echo [ERROR] Compile failed.
    pause
    exit /b 1
)

copy /Y "%SRC_DIR%\UI.fxml" "%OUT_DIR%\UI.fxml" >nul

echo.
echo [2/2] Running...
java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml,javafx.swing -cp "%OUT_DIR%" Main

pause
