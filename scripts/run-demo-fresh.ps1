[CmdletBinding()]
param(
  [switch]$Detach
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$composeArgs = @('-f', 'compose.yml', '-f', 'compose.demo.yml')

Push-Location $repoRoot
try {
  docker compose @composeArgs down -v --remove-orphans

  $upArgs = @('up', '--build')
  if ($Detach) {
    $upArgs += '-d'
  }

  docker compose @composeArgs @upArgs
}
finally {
  Pop-Location
}
