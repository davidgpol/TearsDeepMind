# Configuración
$baseUrl = "http://localhost:8080/api/history"
$rootDir = Join-Path (Get-Location) "TearsMind"

Write-Host "Iniciando Ingesta de Datos Históricos JSON desde: $rootDir (Optimized Method)"

$dateDirs = Get-ChildItem -Path $rootDir -Directory | Where-Object { $_.Name -match "^\d{8}$" }
$total = $dateDirs.Count
$count = 0
$successM = 0
$successQ = 0

foreach ($dir in $dateDirs) {
    $count++
    $rawDate = $dir.Name
    $dateId = $rawDate 
    
    if ($count % 20 -eq 0) { Write-Host "Progreso: [$count / $total]..." }

    # --- 1. Ingestar Market Memory ---
    $marketFile = Join-Path $dir.FullName "market_memory.json"
    if (Test-Path $marketFile) {
        try {
            # USAR InFile para evitar problemas de encoding en memoria
            $response = Invoke-RestMethod -Uri "$baseUrl/daily-analysis/$dateId" -Method Post -InFile $marketFile -ContentType "application/json; charset=utf-8"
            $successM++
        } catch {
            Write-Host "Error ingesting Market [$dateId]: $_" -ForegroundColor Red
        }
    }

    # --- 2. Ingestar Quant Memory ---
    $quantFile = Join-Path $dir.FullName "quant_memory.json"
    if (Test-Path $quantFile) {
        try {
            $response = Invoke-RestMethod -Uri "$baseUrl/quant-memory/$dateId" -Method Post -InFile $quantFile -ContentType "application/json; charset=utf-8"
            $successQ++
        } catch {
            Write-Host "Error ingesting Quant [$dateId]: $_" -ForegroundColor Red
        }
    }
}

Write-Host "Ingesta Finalizada."
Write-Host "Market Reports Ingestados: $successM" -ForegroundColor Green
Write-Host "Quant Reports Ingestados: $successQ" -ForegroundColor Green