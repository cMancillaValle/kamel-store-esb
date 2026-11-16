$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$statePath = Join-Path $repo '.kamel\runtime.json'

Write-Host '========================================' -ForegroundColor DarkYellow
Write-Host '      DETENER KAMEL STORE ESB' -ForegroundColor Yellow
Write-Host '========================================' -ForegroundColor DarkYellow

if (-not (Test-Path -LiteralPath $statePath -PathType Leaf)) {
    Write-Host '[INFO] No hay procesos registrados por Kamel Store.' -ForegroundColor Yellow
    exit 0
}

$state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
if ([System.IO.Path]::GetFullPath($state.ProjectRoot) -ne [System.IO.Path]::GetFullPath($repo)) {
    throw 'El archivo de estado pertenece a otro proyecto. No se detuvo ningun proceso.'
}

$allProcesses = @(Get-CimInstance Win32_Process)

function Get-ProcessTreeLeafFirst {
    param([int]$RootId)
    $result = [System.Collections.Generic.List[int]]::new()
    $visited = [System.Collections.Generic.HashSet[int]]::new()
    function Visit([int]$Id) {
        if (-not $visited.Add($Id)) { return }
        foreach ($child in $allProcesses | Where-Object { $_.ParentProcessId -eq $Id }) {
            Visit ([int]$child.ProcessId)
        }
        $result.Add($Id)
    }
    Visit $RootId
    return $result
}

foreach ($entry in @($state.Processes)) {
    $rootProcess = $allProcesses | Where-Object { $_.ProcessId -eq [int]$entry.ProcessId } | Select-Object -First 1
    if (-not $rootProcess) {
        Write-Host "[INFO] $($entry.Label) ya estaba apagado."
        continue
    }

    $expectedRunner = 'run-component.ps1'
    if ($rootProcess.CommandLine -notlike "*$expectedRunner*" -or $rootProcess.CommandLine -notlike "*$repo*") {
        Write-Host "[AVISO] Se omitio $($entry.Label): el PID registrado ya no pertenece a Kamel Store." -ForegroundColor Yellow
        continue
    }

    foreach ($processId in (Get-ProcessTreeLeafFirst -RootId ([int]$entry.ProcessId))) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
    Write-Host "[OK]   $($entry.Label) detenido." -ForegroundColor Green
}

Remove-Item -LiteralPath $statePath -Force
Write-Host '========================================' -ForegroundColor DarkYellow
Write-Host 'Kamel Store fue detenido.' -ForegroundColor Green
