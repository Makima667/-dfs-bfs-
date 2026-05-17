@echo off
setlocal EnableExtensions

set "PROJECT_DIR=%~dp0"
set "JAVAFX_HOME=C:\Users\q1763\Desktop\javafx-sdk-24.0.2"
set "JAVAFX_LIB=%JAVAFX_HOME%\lib"
set "DIST_DIR=%PROJECT_DIR%dist"
set "APP_DEST=%DIST_DIR%\app"

call "%PROJECT_DIR%build_jar.bat" --nopause
if errorlevel 1 exit /b 1

where jpackage >nul 2>nul
if errorlevel 1 (
    echo [ERROR] jpackage was not found. Please use JDK 17 or higher.
    pause
    exit /b 1
)

if not exist "%APP_DEST%" mkdir "%APP_DEST%"

echo Building app image with jpackage...
jpackage ^
  --type app-image ^
  --name PathLab ^
  --input "%DIST_DIR%" ^
  --main-jar PathLab.jar ^
  --main-class Main ^
  --dest "%APP_DEST%" ^
  --module-path "%JAVAFX_LIB%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.swing

if errorlevel 1 (
    echo [ERROR] jpackage failed.
    pause
    exit /b 1
)

echo.
echo Done: %APP_DEST%\PathLab
pause
