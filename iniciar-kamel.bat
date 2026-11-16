@echo off
setlocal
cd /d "%~dp0"
title Kamel Store - Inicio
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-all.ps1"
set "KAMEL_EXIT=%ERRORLEVEL%"
echo.
if not "%KAMEL_EXIT%"=="0" echo El inicio no se completo. Revisa el mensaje anterior y las ventanas de registro.
pause
exit /b %KAMEL_EXIT%
