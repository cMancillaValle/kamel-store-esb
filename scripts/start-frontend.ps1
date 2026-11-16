$repo = Split-Path $PSScriptRoot -Parent
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw 'npm no está disponible en PATH. Comprueba que npm -v funciona en esta terminal.'
}
Push-Location -LiteralPath (Join-Path $repo 'frontend')
try {
    npm run dev
    if ($LASTEXITCODE -ne 0) {
        throw "npm finalizo con codigo $LASTEXITCODE al iniciar el frontend."
    }
}
finally {
    Pop-Location
}
