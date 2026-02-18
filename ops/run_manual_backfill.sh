#!/bin/bash

# Configuration
API_URL="http://localhost:8080/api/v2/pipeline/run"
VOLUME_PATH="/app/TearsMind"
CONTAINER="tears-app"

# Stats
TOTAL=0
PROCESSED=0
FULL=0
ONLY_QUANT=0
ONLY_MACRO=0
FAILED=0

# List dates from container volume
DATES=$(docker exec $CONTAINER ls $VOLUME_PATH | grep -E '^[0-9]{8}$' | sort)
TOTAL=$(echo "$DATES" | wc -l)

echo "Starting Backfill of $TOTAL dates..."
echo "-----------------------------------"

for FOLDER in $DATES; do
    # Convert YYYYMMDD to YYYY-MM-DD
    DATE="${FOLDER:0:4}-${FOLDER:4:2}-${FOLDER:6:2}"
    PROCESSED=$((PROCESSED + 1))
    
    echo -n "[$PROCESSED/$TOTAL] Processing $DATE... "
    
    # Call API
    RESPONSE=$(curl -s -X POST "$API_URL/$DATE")
    
    if [[ $? -ne 0 || -z "$RESPONSE" ]]; then
        echo "FAILED (Connection Error)"
        FAILED=$((FAILED + 1))
    else
        # Parse simple JSON (status, quant_present, macro_present)
        STATUS=$(echo $RESPONSE | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        QUANT=$(echo $RESPONSE | grep -o '"quant_present":[^,]*' | cut -d':' -f2)
        MACRO=$(echo $RESPONSE | grep -o '"macro_present":[^,}]*' | cut -d':' -f2)
        
        if [[ "$STATUS" == "SUCCESS" ]]; then
            if [[ "$QUANT" == "true" && "$MACRO" == "true" ]]; then
                echo "DONE (FULL)"
                FULL=$((FULL + 1))
            elif [[ "$QUANT" == "true" ]]; then
                echo "DONE (ONLY QUANT)"
                ONLY_QUANT=$((ONLY_QUANT + 1))
            elif [[ "$MACRO" == "true" ]]; then
                echo "DONE (ONLY MACRO)"
                ONLY_MACRO=$((ONLY_MACRO + 1))
            else
                echo "DONE (NO DATA)"
            fi
        else
            echo "FAILED ($STATUS)"
            FAILED=$((FAILED + 1))
        fi
    fi
    
    # Progress Dashboard
    PERCENT=$((PROCESSED * 100 / TOTAL))
    echo "   Progress: $PERCENT% | FULL: $FULL | QUANT: $ONLY_QUANT | MACRO: $ONLY_MACRO | FAILED: $FAILED"
    echo "-----------------------------------"
    
    # Safety sleep for Gemini API rate limits
    sleep 1
done

echo "Backfill Finished!"
echo "Final Summary: Full: $FULL, Only Quant: $ONLY_QUANT, Only Macro: $ONLY_MACRO, Failed: $FAILED"
