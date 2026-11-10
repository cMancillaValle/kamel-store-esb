$repo = Split-Path $PSScriptRoot -Parent
$javaHome = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:JAVA_HOME = $javaHome
$env:Path = (Join-Path $javaHome 'bin') + ';' + $env:Path
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven no está disponible en PATH. Comprueba que mvn -version funciona en esta terminal de Zed.'
}
Set-Location -LiteralPath $repo
mvn clean verify
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Set-Location -LiteralPath (Join-Path $repo 'frontend')
npm run build
exit $LASTEXITCODE
