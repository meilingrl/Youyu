param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$EnvFile = ""
)

if (!$EnvFile) {
    $EnvFile = Join-Path $RepoRoot ".env"
}

if (!(Test-Path $EnvFile)) {
    return
}

Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()

    if (!$line -or $line.StartsWith("#") -or !$line.Contains("=")) {
        return
    }

    $name, $value = $line.Split("=", 2)
    $name = $name.Trim()
    $value = $value.Trim()

    if ($value.Length -ge 2) {
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
    }

    [Environment]::SetEnvironmentVariable($name, $value, "Process")
}
