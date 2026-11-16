param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('inventario', 'facturacion', 'esb', 'ventas', 'frontend')]
    [string]$Component,

    [Parameter(Mandatory = $true)]
    [string]$LogPath
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$host.UI.RawUI.WindowTitle = "Kamel Store - $Component"

Start-Transcript -LiteralPath $LogPath -Append | Out-Null
try {
    Write-Host "========================================" -ForegroundColor DarkYellow
    Write-Host " Kamel Store - $Component" -ForegroundColor Yellow
    Write-Host " Registro: $LogPath"
    Write-Host "========================================" -ForegroundColor DarkYellow

    if ($Component -eq 'frontend') {
        & (Join-Path $PSScriptRoot 'start-frontend.ps1')
    }
    else {
        & (Join-Path $PSScriptRoot 'start-service.ps1') $Component
    }

    if ($LASTEXITCODE -ne 0) {
        throw "El componente $Component finalizo con codigo $LASTEXITCODE."
    }
}
catch {
    Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
    Write-Host 'La ventana permanecera abierta para facilitar la depuracion.' -ForegroundColor Yellow
}
finally {
    Stop-Transcript | Out-Null
}
