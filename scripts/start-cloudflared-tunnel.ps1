param(
    [string]$BackendUrl = "http://127.0.0.1:8080",
    [switch]$UpdateEnv = $true
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$logPath = Join-Path $PSScriptRoot "cloudflared-tunnel.log"
$pidPath = Join-Path $PSScriptRoot "cloudflared-tunnel.pid"
$envPath = Join-Path $repoRoot ".env"

if (Test-Path $pidPath) {
    $existingPid = Get-Content $pidPath -ErrorAction SilentlyContinue
    if ($existingPid) {
        Stop-Process -Id $existingPid -ErrorAction SilentlyContinue
    }
    Remove-Item $pidPath -Force -ErrorAction SilentlyContinue
}

if (Test-Path $logPath) {
    Remove-Item $logPath -Force
}

$proc = Start-Process -FilePath "cloudflared" `
    -ArgumentList @("tunnel", "--url", $BackendUrl, "--logfile", $logPath, "--loglevel", "info") `
    -PassThru `
    -WindowStyle Hidden

Set-Content -Path $pidPath -Value $proc.Id

$publicUrl = $null
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Milliseconds 500
    if (Test-Path $logPath) {
        $match = Select-String -Path $logPath -Pattern 'https://[-a-z0-9]+\.trycloudflare\.com' | Select-Object -Last 1
        if ($match) {
            $publicUrl = [regex]::Match($match.Line, 'https://[-a-z0-9]+\.trycloudflare\.com').Value
            break
        }
    }
}

if (-not $publicUrl) {
    throw "Timed out waiting for cloudflared public URL."
}

$notifyUrl = "$publicUrl/api/payments/callbacks/alipay-sandbox"

if ($UpdateEnv -and (Test-Path $envPath)) {
    $content = Get-Content $envPath -Raw
    $content = [regex]::Replace($content, '^ALIPAY_SANDBOX_ENABLED=.*$', 'ALIPAY_SANDBOX_ENABLED=true', 'Multiline')
    $content = [regex]::Replace($content, '^ALIPAY_SANDBOX_NOTIFY_URL=.*$', "ALIPAY_SANDBOX_NOTIFY_URL=$notifyUrl", 'Multiline')
    Set-Content -Path $envPath -Value $content -NoNewline
}

Write-Host "Cloudflared running. PID: $($proc.Id)"
Write-Host "Public URL: $publicUrl"
Write-Host "Notify URL: $notifyUrl"
