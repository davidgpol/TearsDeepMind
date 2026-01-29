#!/bin/bash
#
# load_reports.sh
#
# Itera sobre los ficheros JSON en el volumen TearsMind y los inserta en la base de datos
# a través de los endpoints de la API /api/history/...
#

REPORTS_DIR="/home/ubuntu/.gemini/Dev/Volumes/TearsMind"
BASE_URL="http://localhost:8080/api/history"
TOTAL_FILES=$(find "$REPORTS_DIR" -type f -name "*.json" | wc -l)
CURRENT_FILE=0
SUCCESS_COUNT=0
ERROR_COUNT=0

echo "Iniciando la carga de $TOTAL_FILES informes..."

# Usamos find y un bucle while read para manejar correctamente nombres de fichero con espacios
find "$REPORTS_DIR" -type f -name "*.json" | while read -r file_path; do
    
    ((CURRENT_FILE++))
    
    # Extraer la fecha del JSON. Salta el fichero si no hay fecha.
    report_date=$(jq -r '.date' "$file_path")
    if [ -z "$report_date" ] || [ "$report_date" == "null" ]; then
        echo "[$CURRENT_FILE/$TOTAL_FILES] ERROR: No se encontró fecha en '$file_path'. Saltando."
        ((ERROR_COUNT++))
        continue
    fi

    # Determinar el endpoint basándose en el nombre del fichero
    endpoint=""
    if [[ "$file_path" == *"quant_memory"* ]]; then
        endpoint="$BASE_URL/quant-memory/$report_date"
    elif [[ "$file_path" == *"market_memory"* ]]; then
        endpoint="$BASE_URL/daily-analysis/$report_date"
    else
        echo "[$CURRENT_FILE/$TOTAL_FILES] ADVERTENCIA: Tipo de informe desconocido para '$file_path'. Saltando."
        ((ERROR_COUNT++))
        continue
    fi

    echo -n "[$CURRENT_FILE/$TOTAL_FILES] Cargando '$file_path' a '$endpoint'... "

    # Enviar el contenido del fichero a la API
    response=$(curl -s -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        --data @"$file_path" \
        "$endpoint")

    # Extraer el código HTTP de la respuesta
    http_code="${response: -3}"

    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo "ÉXITO (HTTP $http_code)"
        ((SUCCESS_COUNT++))
    else
        body="${response:0:${#response}-3}"
        echo "FALLO (HTTP $http_code) - Respuesta: $body"
        ((ERROR_COUNT++))
    fi
    
    # Pequeña pausa para no saturar el servidor
    sleep 0.1
done

echo "---"
echo "Carga finalizada."
echo "Informes cargados con éxito: $SUCCESS_COUNT"
echo "Informes con errores o saltados: $ERROR_COUNT"
