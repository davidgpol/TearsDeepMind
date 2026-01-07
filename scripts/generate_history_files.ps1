# Configuración
$rootDir = Join-Path (Get-Location) "TearsMind"
Write-Host "TearAgent: Iniciando generación masiva de memorias en $rootDir"

# Esquemas Base (Simplificados para migración histórica)
$marketTemplate = @{
    date = ""
    regime = @{
        trend = "LEGACY_DATA"
        volatility = "LEGACY_DATA"
        environment = "LEGACY_DATA"
    }
    macro_bias = @{
        fed_policy = "UNKNOWN"
    }
    daily_thesis = @{
        expected_behavior = "SEE_RAW_CONTENT"
    }
    raw_report_content = ""
}

$quantTemplate = @{
    date = ""
    quant_state = @{
        directional_bias = "LEGACY_DATA"
        risk_level = "UNKNOWN"
    }
    quant_levels = @{
        support = @()
        resistance = @()
    }
    raw_report_content = ""
}

# Obtener carpetas de fecha
$dateDirs = Get-ChildItem -Path $rootDir -Directory | Where-Object { $_.Name -match "^\d{8}$" }
$total = $dateDirs.Count
$count = 0

foreach ($dir in $dateDirs) {
    $count++
    $date = $dir.Name
    
    # Progreso periódico
    if ($count % 10 -eq 0) {
        Write-Host "Procesando... [$count / $total] - $date"
    }

    # --- 1. Generar Market Memory ---
    $dailyPath = Join-Path $dir.FullName "DailyAnalysis"
    if (Test-Path $dailyPath) {
        $txtFile = Get-ChildItem -Path $dailyPath -Filter "*.txt" | Select-Object -First 1
        if ($txtFile) {
            $content = Get-Content -Path $txtFile.FullName -Raw -Encoding UTF8
            
            # Clonar y rellenar template
            $json = $marketTemplate.Clone()
            $json.date = $date
            $json.raw_report_content = $content
            
            # Guardar market_memory.json en la raíz del día (TearsMind/YYYYMMDD/market_memory.json)
            $outFile = Join-Path $dir.FullName "market_memory.json"
            $json | ConvertTo-Json -Depth 10 -Compress | Set-Content -Path $outFile -Encoding UTF8
        }
    }

    # --- 2. Generar Quant Memory ---
    $quantPath = Join-Path $dir.FullName "QuantUpdates"
    if (Test-Path $quantPath) {
        $txtFile = Get-ChildItem -Path $quantPath -Filter "*.txt" | Select-Object -First 1
        if ($txtFile) {
            $content = Get-Content -Path $txtFile.FullName -Raw -Encoding UTF8
            
            # Clonar y rellenar template
            $json = $quantTemplate.Clone()
            $json.date = $date
            $json.raw_report_content = $content
            
            # Guardar quant_memory.json en la raíz del día
            $outFile = Join-Path $dir.FullName "quant_memory.json"
            $json | ConvertTo-Json -Depth 10 -Compress | Set-Content -Path $outFile -Encoding UTF8
        }
    }
}

Write-Host "TearAgent: Procesamiento completado. $count días procesados."
