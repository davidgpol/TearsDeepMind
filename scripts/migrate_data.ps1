# Configuración
$baseUrl = "http://localhost:8080/api/history"
$rootDir = Join-Path (Get-Location) "TearsMind"

Write-Host "Iniciando Migración Masiva desde: $rootDir"

# Obtener carpetas de fecha (formato 8 dígitos)
$dateDirs = Get-ChildItem -Path $rootDir -Directory | Where-Object { $_.Name -match "^\d{8}$" }
$total = $dateDirs.Count
$current = 0

foreach ($dir in $dateDirs) {
    $current++
    $date = $dir.Name
    Write-Host "Procesando [$current / $total] Fecha: $date" -NoNewline

    # --- 1. Daily Analysis ---
    $dailyPath = Join-Path $dir.FullName "DailyAnalysis"
    if (Test-Path $dailyPath) {
        $txtFile = Get-ChildItem -Path $dailyPath -Filter "*.txt" | Select-Object -First 1
        if ($txtFile) {
            $content = Get-Content -Path $txtFile.FullName -Raw -Encoding UTF8
            
            # Construir Payload JSON
            $payload = @{
                source_file = $txtFile.Name
                raw_content = $content
                migrated_at = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
            } | ConvertTo-Json -Depth 10 -Compress

            # Enviar Request
            try {
                $response = Invoke-RestMethod -Uri "$baseUrl/daily-analysis/$date" -Method Post -Body $payload -ContentType "application/json"
                Write-Host " [Daily: OK]" -ForegroundColor Green -NoNewline
            } catch {
                Write-Host " [Daily: ERROR $_]" -ForegroundColor Red -NoNewline
            }
        } else {
            Write-Host " [Daily: No File]" -ForegroundColor Yellow -NoNewline
        }
    }

    # --- 2. Quant Memory ---
    $quantPath = Join-Path $dir.FullName "QuantUpdates"
    if (Test-Path $quantPath) {
        $txtFile = Get-ChildItem -Path $quantPath -Filter "*.txt" | Select-Object -First 1
        if ($txtFile) {
            $content = Get-Content -Path $txtFile.FullName -Raw -Encoding UTF8
            
            # Construir Payload JSON
            $payload = @{
                source_file = $txtFile.Name
                raw_content = $content
                migrated_at = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
            } | ConvertTo-Json -Depth 10 -Compress

            # Enviar Request
            try {
                $response = Invoke-RestMethod -Uri "$baseUrl/quant-memory/$date" -Method Post -Body $payload -ContentType "application/json"
                Write-Host " [Quant: OK]" -ForegroundColor Green -NoNewline
            } catch {
                Write-Host " [Quant: ERROR $_]" -ForegroundColor Red -NoNewline
            }
        } else {
            Write-Host " [Quant: No File]" -ForegroundColor Yellow -NoNewline
        }
    }
    
    Write-Host "" # Nueva línea
}

Write-Host "Migración Finalizada."
