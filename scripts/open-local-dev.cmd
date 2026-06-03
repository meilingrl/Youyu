@echo off
setlocal

set "SCRIPT_DIR=%~dp0"

start "Youyu Backend" powershell.exe -NoExit -ExecutionPolicy Bypass -File "%SCRIPT_DIR%start-backend-local.ps1"
start "Youyu Frontend" powershell.exe -NoExit -ExecutionPolicy Bypass -File "%SCRIPT_DIR%start-frontend-local.ps1"

endlocal
