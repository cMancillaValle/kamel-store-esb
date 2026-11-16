$repo = Split-Path $PSScriptRoot -Parent
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven no está disponible en PATH. Comprueba que mvn -version funciona en esta terminal de Zed.'
}
Push-Location -LiteralPath $repo
try {
    mvn clean verify
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Set-Location -LiteralPath (Join-Path $repo 'frontend')
    npm run build
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
