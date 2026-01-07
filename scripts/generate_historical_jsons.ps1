# Configuration
$RootDir = "TearsDeepMind/TearsMind"
$SkipExisting = $false

# Schemas
function Get-MarketMemoryTemplate($dateStr) {
    return [Ordered]@{
        "date" = $dateStr
        "regime" = [Ordered]@{
            "trend" = "neutral"
            "volatility" = "normal"
            "volume" = "normal"
            "environment" = "normal"
        }
        "macro_bias" = [Ordered]@{
            "fed_policy" = "neutral"
            "rate_expectation" = "mixed"
            "fiscal_stance" = "neutral"
            "usd_bias" = "neutral"
        }
        "market_structure" = [Ordered]@{
            "breadth" = "unknown"
            "advance_decline" = "unknown"
            "participation" = "unknown"
        }
        "spx_structure" = [Ordered]@{
            "key_support" = @()
            "key_resistance" = @()
            "upside_targets" = @()
            "downside_targets" = @()
        }
        "volatility_state" = [Ordered]@{
            "vix_state" = "unknown"
            "dealer_gamma" = "unknown"
            "volatility_risk" = "contained"
        }
        "daily_thesis" = [Ordered]@{
            "expected_behavior" = "uncertain"
            "dominant_risk" = "unknown"
            "invalidation_conditions" = @()
        }
    }
}

function Get-QuantMemoryTemplate($dateStr) {
    return [Ordered]@{
        "date" = $dateStr
        "quant_levels" = [Ordered]@{
            "support" = @()
            "resistance" = @()
            "reversal_zones" = @()
            "extreme_levels" = @()
        }
        "level_roles" = [Ordered]@{
            "mean_reversion" = @()
            "high_probability_reversal" = @()
            "breakout_levels" = @()
        }
        "quant_state" = [Ordered]@{
            "directional_bias" = "neutral"
            "preferred_play" = "wait"
            "risk_level" = "medium"
        }
    }
}

# Parsing Logic
function Parse-MarketFile($filePath, $dateStr) {
    $text = (Get-Content $filePath -Raw).ToLower()
    $data = Get-MarketMemoryTemplate $dateStr

    # 1. Regime
    if ($text -match "range|chop|sideways|flat|correction in time") { $data.regime.trend = "range" }
    elseif ($text -match "bullish|uptrend|rally|breakout|highs") { $data.regime.trend = "bullish" }
    elseif ($text -match "bearish|downtrend|selloff|correction|decline") { $data.regime.trend = "bearish" }

    # 2. Levels (Regex)
    $supports = [regex]::Matches($text, "support.*?(\d{4})") | ForEach-Object { [int]$_.Groups[1].Value }
    $resistances = [regex]::Matches($text, "resistance.*?(\d{4})") | ForEach-Object { [int]$_.Groups[1].Value }

    # Fallback
    if ($supports.Count -eq 0 -and $resistances.Count -eq 0) {
        $allLevels = [regex]::Matches($text, "\b(4\d{3}|5\d{3}|6\d{3})\b") | ForEach-Object { [int]$_.Groups[1].Value }
        if ($allLevels.Count -gt 0) {
            $mid = ($allLevels | Measure-Object -Average).Average
            $supports = $allLevels | Where-Object { $_ -lt $mid }
            $resistances = $allLevels | Where-Object { $_ -ge $mid }
        }
    }

    $data.spx_structure.key_support = @($supports | Select-Object -First 3)
    $data.spx_structure.key_resistance = @($resistances | Select-Object -First 3)

    return $data
}

function Parse-QuantFile($filePath, $dateStr) {
    $lines = Get-Content $filePath
    $data = Get-QuantMemoryTemplate $dateStr
    $allLevels = @()

    foreach ($line in $lines) {
        $l = $line.Trim().ToLower()
        if ($l -match "^(\d{4})") {
            $level = [int]$Matches[1]
            $allLevels += $level
            
            if ($l -match "reversal") { 
                $data.quant_levels.reversal_zones += $level 
                $data.level_roles.mean_reversion += $level
            }
            if ($l -match "pivot") { $data.level_roles.breakout_levels += $level }
        }
        
        if ($l -match "support:") {
            ([regex]::Matches($l, "(\d{4})") | ForEach-Object { [int]$_.Groups[1].Value }) | ForEach-Object { $data.quant_levels.support += $_ }
        }
        if ($l -match "resistance:") {
            ([regex]::Matches($l, "(\d{4})") | ForEach-Object { [int]$_.Groups[1].Value }) | ForEach-Object { $data.quant_levels.resistance += $_ }
        }
    }

    if ($allLevels.Count -gt 0) {
        $sorted = $allLevels | Sort-Object | Select-Object -Unique
        if ($data.quant_levels.support.Count -eq 0) { $data.quant_levels.support = @($sorted | Select-Object -First 2) }
        if ($data.quant_levels.resistance.Count -eq 0) { $data.quant_levels.resistance = @($sorted | Select-Object -Last 2) }
        if ($data.quant_levels.extreme_levels.Count -eq 0) { 
             $data.quant_levels.extreme_levels = @($sorted[0], $sorted[-1])
        }
    }
    
    if ($data.level_roles.breakout_levels.Count -gt 0) {
        $data.quant_state.preferred_play = "breakout"
    } else {
        $data.quant_state.preferred_play = "fade"
    }

    return $data
}

function Format-JsonCleanup($jsonStr) {
    # 1. First minify slightly to remove PowerShell's excessive gaps after colons
    # PowerShell ConvertTo-Json uses many spaces. We replace ":  " with ": "
    $jsonStr = $jsonStr -replace '"":\s+', '"": ' # Corrected: escaped the colon and added space
    
    # 2. Collapse arrays (even empty ones or with content)
    # This regex looks for [ followed by any amount of whitespace and then anything that isn't a bracket or brace
    $pattern = '\[\s*([^\[\]{}]*?)\s*\]' # Corrected: escaped brackets
    $callback = {
        param($match)
        $content = $match.Groups[1].Value -replace '\r?\n\s*', ' ' # Corrected: escaped backslash and newline
        $content = $content -replace '\s+', ' ' # Corrected: escaped backslash
        return "[" + $content.Trim() + "]"
    }
    $jsonStr = [regex]::Replace($jsonStr, $pattern, $callback)
    
    return $jsonStr
}

# Main Execution
Write-Host "Starting historical JSON generation..."
$dirs = Get-ChildItem -Path $RootDir -Directory

foreach ($dir in $dirs) {
    $dateStrRaw = $dir.Name
    try {
        $dateObj = [DateTime]::ParseExact($dateStrRaw, "yyyyMMdd", $null)
        $formattedDate = $dateObj.ToString("yyyy-MM-dd")
    } catch {
        Write-Host "Skipping invalid folder: $dateStrRaw"
        continue
    }

    # Market Memory
    $marketJsonPath = Join-Path $dir.FullName "market_memory.json"
    $dailyAnalysisDir = Join-Path $dir.FullName "DailyAnalysis"
    
    if (-not ($SkipExisting -and (Test-Path $marketJsonPath))) {
        if (Test-Path $dailyAnalysisDir) {
            $txtFiles = Get-ChildItem -Path $dailyAnalysisDir -Filter "*.txt"
            if ($txtFiles.Count -gt 0) {
                try {
                    $jsonObj = Parse-MarketFile $txtFiles[0].FullName $formattedDate
                    $jsonRaw = $jsonObj | ConvertTo-Json -Depth 10
                    $jsonFormatted = Format-JsonCleanup $jsonRaw
                    $jsonFormatted | Set-Content $marketJsonPath
                    Write-Host "[$dateStrRaw] Generated market_memory.json"
                } catch {
                    Write-Host "[$dateStrRaw] Error parsing market file: $_ "
                }
            }
        }
    }

    # Quant Memory
    $quantJsonPath = Join-Path $dir.FullName "quant_memory.json"
    $quantUpdatesDir = Join-Path $dir.FullName "QuantUpdates"

    if (-not ($SkipExisting -and (Test-Path $quantJsonPath))) {
        if (Test-Path $quantUpdatesDir) {
            $txtFiles = Get-ChildItem -Path $quantUpdatesDir -Filter "*.txt"
            if ($txtFiles.Count -gt 0) {
                try {
                    $jsonObj = Parse-QuantFile $txtFiles[0].FullName $formattedDate
                    $jsonRaw = $jsonObj | ConvertTo-Json -Depth 10
                    $jsonFormatted = Format-JsonCleanup $jsonRaw
                    $jsonFormatted | Set-Content $quantJsonPath
                    Write-Host "[$dateStrRaw] Generated quant_memory.json"
                } catch {
                    Write-Host "[$dateStrRaw] Error parsing quant file: $_ "
                }
            }
        }
    }
}

Write-Host "Done."