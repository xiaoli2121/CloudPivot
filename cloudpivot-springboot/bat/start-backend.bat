@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_DIR=%%~fI"
set "BOOT_MODULE_DIR=%PROJECT_DIR%\cloudpivot-boot"
set "JAR_FILE=%BOOT_MODULE_DIR%\target\cloudpivot-boot-0.1.0-SNAPSHOT.jar"

cd /d "%PROJECT_DIR%"

echo [CloudPivot] Building backend modules...
call mvn -pl cloudpivot-boot -am package -DskipTests
if errorlevel 1 (
  echo [CloudPivot] Backend build failed. Please review the Maven output above.
  exit /b 1
)

if not exist "%JAR_FILE%" (
  echo [CloudPivot] Backend package not found: %JAR_FILE%
  exit /b 1
)

echo [CloudPivot] Starting backend from %JAR_FILE%
java -jar "%JAR_FILE%"
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
  echo [CloudPivot] Backend exited with code %EXIT_CODE%.
)

exit /b %EXIT_CODE%
