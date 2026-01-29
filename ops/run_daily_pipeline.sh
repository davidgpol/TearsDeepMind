#!/bin/bash

# Configuration
LOG_DIR="/home/ubuntu/.gemini/Dev/Volumes/TearsMind/logs/ops"
API_URL="http://localhost:8080/api/v2/pipeline/run"
MONTH_STR=$(date +"%Y-%m")
LOG_FILE="${LOG_DIR}/pipeline_${MONTH_STR}.log"

# Initialization
mkdir -p "$LOG_DIR"

log() {
    local level=$1
    local message=$2
    echo "$(date +"%Y-%m-%d %H:%M:%S") [$level] $message" >> "$LOG_FILE"
}

CURRENT_DATE=$(date +"%Y-%m-%d")
log "INFO" "--- Starting Daily Pipeline for ${CURRENT_DATE} ---"

# Step 1: Healthcheck
if ! curl -s --head --request GET http://localhost:8080/api/history/daily-analysis/health | grep "200" > /dev/null; then
    # Fallback to simple port check if health endpoint not ready
    if ! nc -z localhost 8080; then
        log "ERROR" "TearsDeepMind API is not responding on port 8080. Aborting."
        exit 1
    fi
fi

# Step 2: Trigger Pipeline
log "INFO" "Triggering pipeline via POST ${API_URL}/${CURRENT_DATE}"
START_TIME=$(date +%s)

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${API_URL}/${CURRENT_DATE}")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# Step 3: Result Analysis
if [ "$HTTP_CODE" -eq 200 ]; then
    log "INFO" "Pipeline executed successfully. Duration: ${DURATION}s. Response: $BODY"
else
    log "ERROR" "Pipeline failed with HTTP ${HTTP_CODE}. Duration: ${DURATION}s. Error: $BODY"
    exit 1
fi

log "INFO" "--- Daily Pipeline Finished ---"
