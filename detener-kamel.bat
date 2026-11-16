@echo off
setlocal
cd /d "%~dp0"
title Kamel Store - Detencion
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\stop-all.ps1"
set "KAMEL_EXIT=%ERRORLEVEL%"
echo.
pause
exit /b %KAMEL_EXIT%
