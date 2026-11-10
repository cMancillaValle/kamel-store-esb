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

$javaHome = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
if (-not (Test-Path -LiteralPath (Join-Path $javaHome 'bin/java.exe') -PathType Leaf)) {
    throw 'No se encontró Java 21. Ajusta $javaHome en este script.'
}
$env:JAVA_HOME = $javaHome
$env:Path = (Join-Path $javaHome 'bin') + ';' + $env:Path

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven no está disponible en PATH. Comprueba que mvn -version funciona en esta terminal de Zed.'
}

Set-Location -LiteralPath $serviceDir
mvn spring-boot:run
