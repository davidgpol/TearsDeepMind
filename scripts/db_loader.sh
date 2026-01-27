#!/bin/bash

BASE_DIR="/home/ubuntu/.gemini/Dev/Volumes/TearsMind"
API_URL="http://localhost:8080/api/history"
ERROR_LOG="/home/ubuntu/.gemini/Dev/Projects/TearsDeepMind/scripts/ingestion_errors.log"
SUCCESS_COUNT=0
FAILURE_COUNT=0

# Limpiar log de errores previo
echo "[$(date)] Iniciando nueva sesión de ingesta" > "$ERROR_LOG"

echo "--------------------------------------------------"
echo "🚀 Iniciando Ingesta Masiva en Base de Datos"
echo "--------------------------------------------------"

for date_folder in $(ls "$BASE_DIR" | grep -E '^[0-9]{8}$'); do
    # Formatear fecha para la API (YYYYMMDD -> YYYY-MM-DD)
    YEAR=${date_folder:0:4}
    MONTH=${date_folder:4:2}
    DAY=${date_folder:6:2}
    API_DATE="$YEAR-$MONTH-$DAY"
    
    FULL_PATH="$BASE_DIR/$date_folder"
    
    # 1. Procesar Market Memory (Daily Analysis)
    MARKET_FILE=$(find "$FULL_PATH" -name "market_memory_*.json" | head -n 1)
    if [ -f "$MARKET_FILE" ]; then
        RESPONSE=$(curl -s -w "%{http_code}" -X POST "$API_URL/daily-analysis/$API_DATE" \
            -H "Content-Type: application/json" \
            -d @"$MARKET_FILE")
        
        HTTP_CODE="${RESPONSE: -3}"
        
        if [[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "201" ]]; then
            ((SUCCESS_COUNT++))
        else
            ((FAILURE_COUNT++))
            echo "[$(date)] ERROR: DailyAnalysis $API_DATE - HTTP $HTTP_CODE - File: $MARKET_FILE" >> "$ERROR_LOG"
        fi
    fi

    # 2. Procesar Quant Memory
    QUANT_FILE=$(find "$FULL_PATH" -name "quant_memory_*.json" | head -n 1)
    if [ -f "$QUANT_FILE" ]; then
        RESPONSE=$(curl -s -w "%{http_code}" -X POST "$API_URL/quant-memory/$API_DATE" \
            -H "Content-Type: application/json" \
            -d @"$QUANT_FILE")
        
        HTTP_CODE="${RESPONSE: -3}"
        
        if [[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "201" ]]; then
            ((SUCCESS_COUNT++))
        else
            ((FAILURE_COUNT++))
            echo "[$(date)] ERROR: QuantMemory $API_DATE - HTTP $HTTP_CODE - File: $QUANT_FILE" >> "$ERROR_LOG"
        fi
    fi
done

echo "--------------------------------------------------"
echo "📊 RESUMEN DE INGESTA"
echo "--------------------------------------------------"
echo "✅ Inserciones Exitosas: $SUCCESS_COUNT"
echo "❌ Inserciones Fallidas: $FAILURE_COUNT"
echo "--------------------------------------------------"
if [ $FAILURE_COUNT -gt 0 ]; then
    echo "⚠️ Revisa el log de errores en: $ERROR_LOG"
fi
