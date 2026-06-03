$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "load-local-env.ps1") -RepoRoot $repoRoot

Push-Location (Join-Path $repoRoot "backend")
try {
    & .\mvnw.cmd spring-boot:run @args
} finally {
    Pop-Location
}
