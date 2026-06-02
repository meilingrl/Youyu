param(
    [int]$FrontendPort = 5173
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

Write-Host "Repo root: $repoRoot"
Write-Host ""
Write-Host "Start backend in terminal A:"
Write-Host "  powershell -ExecutionPolicy Bypass -File `"$repoRoot\scripts\start-backend-local.ps1`""
Write-Host ""
Write-Host "Start frontend in terminal B:"
Write-Host "  powershell -ExecutionPolicy Bypass -File `"$repoRoot\scripts\start-frontend-local.ps1`" -Port $FrontendPort"
Write-Host ""
Write-Host "After startup:"
Write-Host "  Frontend: http://127.0.0.1:$FrontendPort"
Write-Host "  Backend:  http://127.0.0.1:8080"
