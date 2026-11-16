$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$runtimeDir = Join-Path $repo '.kamel'
$logDir = Join-Path $runtimeDir 'logs'
$statePath = Join-Path $runtimeDir 'runtime.json'

$components = @(
    [ordered]@{ Name = 'inventario'; Label = 'Inventario'; Port = 8083; Url = 'http://localhost:8083/api/inventory/products'; Timeout = 120 },
    [ordered]@{ Name = 'facturacion'; Label = 'Facturacion'; Port = 8084; Url = 'http://localhost:8084/api/invoices'; Timeout = 120 },
    [ordered]@{ Name = 'esb'; Label = 'ESB'; Port = 8082; Url = 'http://localhost:8082/api/esb/products'; Timeout = 120 },
    [ordered]@{ Name = 'ventas'; Label = 'Ventas'; Port = 8081; Url = 'http://localhost:8081/api/products'; Timeout = 120 },
    [ordered]@{ Name = 'frontend'; Label = 'Frontend'; Port = 5173; Url = 'http://localhost:5173'; Timeout = 90 }
)

function Test-Port {
    param([int]$Port)
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $result = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        return $result.AsyncWaitHandle.WaitOne(350) -and $client.Connected
    }
    catch { return $false }
    finally { $client.Dispose() }
}

function Wait-Endpoint {
    param([string]$Url, [int]$TimeoutSeconds)
    $limit = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 4
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) { return $true }
        }
        catch { }
        Start-Sleep -Seconds 2
    } while ([DateTime]::UtcNow -lt $limit)
    return $false
}

function Save-State {
    param([array]$Processes)
    $state = [ordered]@{
        ProjectRoot = $repo
        StartedAt = (Get-Date).ToString('o')
        Processes = $Processes
    }
    $state | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $statePath -Encoding UTF8
}

Write-Host '========================================' -ForegroundColor DarkYellow
Write-Host '          KAMEL STORE ESB' -ForegroundColor Yellow
Write-Host '========================================' -ForegroundColor DarkYellow
Write-Host

foreach ($command in @('java', 'mvn', 'node', 'npm')) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "No se encontro '$command' en PATH. Corrige el entorno de esta terminal antes de iniciar Kamel Store."
    }
}

foreach ($component in $components) {
    if (Test-Port $component.Port) {
        throw "El puerto $($component.Port) de $($component.Label) ya esta ocupado. No se iniciaron instancias duplicadas."
    }
}

New-Item -ItemType Directory -Path $logDir -Force | Out-Null
$started = @()
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'

foreach ($component in $components) {
    Write-Host "[....] Iniciando $($component.Label) en el puerto $($component.Port)..."
    $logPath = Join-Path $logDir "$stamp-$($component.Name).log"
    $runner = Join-Path $PSScriptRoot 'run-component.ps1'
    $arguments = @(
        '-NoExit',
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-File', ('"{0}"' -f $runner),
        '-Component', $component.Name,
        '-LogPath', ('"{0}"' -f $logPath)
    )
    $process = Start-Process -FilePath 'powershell.exe' -ArgumentList $arguments -WorkingDirectory $repo -PassThru
    $started += [ordered]@{
        Name = $component.Name
        Label = $component.Label
        Port = $component.Port
        ProcessId = $process.Id
        LogPath = $logPath
    }
    Save-State $started

    if (-not (Wait-Endpoint -Url $component.Url -TimeoutSeconds $component.Timeout)) {
        Write-Host "[ERROR] $($component.Label) no respondio en $($component.Url) dentro del tiempo maximo." -ForegroundColor Red
        Write-Host "Registro: $logPath" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "[OK]   $($component.Label.PadRight(12)) - Puerto $($component.Port)" -ForegroundColor Green
}

Write-Host
Write-Host 'Kamel Store esta disponible en:' -ForegroundColor Green
Write-Host 'http://localhost:5173' -ForegroundColor Cyan
Write-Host
Write-Host "Registros: $logDir"
Write-Host 'Para detener el entorno usa .\detener-kamel.bat'
Write-Host '========================================' -ForegroundColor DarkYellow
