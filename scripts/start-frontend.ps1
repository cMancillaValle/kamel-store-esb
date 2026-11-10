$repo = Split-Path $PSScriptRoot -Parent
Set-Location (Join-Path $repo 'frontend')
npm run dev
