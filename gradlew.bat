@echo off
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle is not installed. Use the GitHub Actions workflow, which installs Gradle 8.7.
  exit /b 127
)
gradle %*
