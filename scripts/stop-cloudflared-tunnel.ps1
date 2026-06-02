$ErrorActionPreference = "Stop"

$pidPath = Join-Path $PSScriptRoot "cloudflared-tunnel.pid"

if (!(Test-Path $pidPath)) {
    Write-Host "No saved cloudflared PID file found."
    exit 0
}

$pid = Get-Content $pidPath -ErrorAction SilentlyContinue
if ($pid) {
    Stop-Process -Id $pid -ErrorAction SilentlyContinue
}

Remove-Item $pidPath -Force -ErrorAction SilentlyContinue
Write-Host "Cloudflared tunnel stopped."
