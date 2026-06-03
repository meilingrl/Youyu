param(
    [int]$Port = 5173
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "load-local-env.ps1") -RepoRoot $repoRoot

Push-Location (Join-Path $repoRoot "frontend")
try {
    if (!(Test-Path "node_modules")) {
        npm install
    }

    npm run dev -- --host
} finally {
    Pop-Location
}
