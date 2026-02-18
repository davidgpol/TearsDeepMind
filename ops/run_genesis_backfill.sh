#!/bin/bash

LOG_FILE="logs/genesis_backfill.log"
API_URL="http://localhost:8080/api/v2/pipeline/run"
START_DATE="2026-01-01"
END_DATE="2026-02-16"

# Create logs dir if not exists
mkdir -p logs

echo "Starting Operation Genesis 2026 (Robust Python Iterator)..." | tee -a $LOG_FILE
echo "From: $START_DATE to $END_DATE" | tee -a $LOG_FILE
echo "----------------------------------------" | tee -a $LOG_FILE

# Generate dates using Python for safety
DATES=$(python3 -c "
from datetime import date, timedelta
d1 = date.fromisoformat('$START_DATE')
d2 = date.fromisoformat('$END_DATE')
delta = d2 - d1
for i in range(delta.days + 1):
    print((d1 + timedelta(days=i)).isoformat())
")

for current_date in $DATES; do
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Processing date: $current_date" | tee -a $LOG_FILE
    
    # Execute Pipeline with timeout
    response=$(curl -s --max-time 300 -X POST "$API_URL/$current_date")
    curl_exit_code=$?

    if [ $curl_exit_code -ne 0 ]; then
        echo "❌ FAILED: curl error code $curl_exit_code for $current_date" | tee -a $LOG_FILE
        continue
    fi
    
    # Check Status
    if [[ $response == *"SUCCESS"* ]]; then
        echo "✅ SUCCESS: $current_date" | tee -a $LOG_FILE
    elif [[ $response == *"No data available"* ]]; then
        echo "⚠️  NO DATA: $current_date (Likely Weekend/Holiday)" | tee -a $LOG_FILE
    else
        echo "❌ FAILED: $current_date - Response: $response" | tee -a $LOG_FILE
    fi
    
    # Market Data Integrity Check (Wait for async process to finish ~2s)
    sleep 2
    
    # Simple check for candles count via psql
    candle_count=$(docker exec tears-db psql -U postgres -d tearsmind -t -c "SELECT COUNT(*) FROM market_data.daily_candles WHERE date = '$current_date';" | tr -d '[:space:]')
    
    if [[ "$candle_count" -gt 0 ]]; then
        echo "   📊 MARKET DATA: Validated ($candle_count daily)" | tee -a $LOG_FILE
    else
         # Only warn if not weekend
         day_of_week=$(date -d "$current_date" +%u)
         if [ "$day_of_week" -lt 6 ]; then
             echo "   🚨 MARKET DATA MISSING for work day $current_date!" | tee -a $LOG_FILE
         fi
    fi
    
    echo "----------------------------------------" | tee -a $LOG_FILE
    
    # Rate Limit Protection
    sleep 3
done

echo "Operation Genesis 2026 Completed." | tee -a $LOG_FILE
