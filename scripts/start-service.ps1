param([Parameter(Mandatory=$true)][string]$Service)

$services = @('ventas', 'esb', 'inventario', 'facturacion')
if ($Service -notin $services) {
    throw "Servicio '$Service' no existe. Opciones: $($services -join ', ')."
}

$repo = Split-Path $PSScriptRoot -Parent
$serviceDir = Join-Path $repo "services/$Service-service"
if (-not (Test-Path -LiteralPath (Join-Path $serviceDir 'pom.xml') -PathType Leaf)) {
    throw "No existe el servicio '$Service' en $serviceDir."
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven no está disponible en PATH. Comprueba que mvn -version funciona en esta terminal de Zed.'
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java no está disponible en PATH. Comprueba que java -version funciona en esta terminal de Zed.'
}

Push-Location -LiteralPath $serviceDir
try {
    mvn spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "Maven finalizo con codigo $LASTEXITCODE al iniciar $Service."
    }
}
finally {
    Pop-Location
}
