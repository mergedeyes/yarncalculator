@echo off
rem Starts Yarn Calculator. Needs a Java JDK (version 17 or newer).
cd /d "%~dp0"
where javaw >nul 2>nul
if errorlevel 1 (
  echo Java was not found. Please install a JDK from https://adoptium.net/ and try again.
  echo Java wurde nicht gefunden. Bitte ein JDK von https://adoptium.net/ installieren.
  pause
  exit /b 1
)
start "" javaw YarnCalculator.java
