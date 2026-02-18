import requests
import time
from datetime import date, timedelta
import os
import subprocess

START_DATE = date.fromisoformat("2026-01-01")
END_DATE = date.fromisoformat("2026-02-16")
API_URL = "http://localhost:8080/api/v2/pipeline/run"
LOG_FILE = "logs/genesis_backfill.log"

# Append mode for log
def log(message):
    print(message)
    with open(LOG_FILE, "a") as f:
        f.write(message + "\n")

log(f"Starting Operation Genesis 2026 (Python Native)...")
log(f"From: {START_DATE} to {END_DATE}")
log("-" * 40)

current_date = START_DATE
while current_date <= END_DATE:
    date_str = current_date.isoformat()
    log(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] Processing date: {date_str}")
    
    try:
        response = requests.post(f"{API_URL}/{date_str}", timeout=300)
        status_code = response.status_code
        try:
            json_resp = response.json()
            status = json_resp.get("status", "UNKNOWN")
            error = json_resp.get("error", "")
        except:
            status = "Non-JSON"
            error = response.text[:100]

        if status == "SUCCESS":
            log(f"✅ SUCCESS: {date_str}")
        elif "No data available" in error:
            log(f"⚠️  NO DATA: {date_str} (Likely Weekend/Holiday)")
        else:
            log(f"❌ FAILED: {date_str} - Status: {status} - Error: {error}")
            
    except Exception as e:
        log(f"❌ NETWORK ERROR: {date_str} - {str(e)}")
    
    # Market Data Integrity Check
    time.sleep(2)
    try:
        cmd = f'docker exec tears-db psql -U postgres -d tearsmind -t -c "SELECT COUNT(*) FROM market_data.daily_candles WHERE date = \'{date_str}\';"'
        result = subprocess.check_output(cmd, shell=True).decode().strip()
        candle_count = int(result) if result.isdigit() else 0
        
        if candle_count > 0:
            log(f"   📊 MARKET DATA: Validated ({candle_count} daily)")
        elif current_date.weekday() < 5: # Mon-Fri
            log(f"   🚨 MARKET DATA MISSING for work day {date_str}!")
            
    except Exception as e:
        log(f"   ⚠️ Market Check Failed: {str(e)}")

    log("-" * 40)
    current_date += timedelta(days=1)
    time.sleep(3)

log("Operation Genesis 2026 Completed.")
