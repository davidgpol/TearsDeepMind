import os
import json
import re
from pathlib import Path
from datetime import datetime

# Configuration
ROOT_DIR = Path("TearsDeepMind/TearsMind")
SKIP_EXISTING = True

# Schemas
def get_market_memory_template(date_str):
    return {
        "date": date_str,
        "regime": {
            "trend": "neutral",
            "volatility": "normal",
            "volume": "normal",
            "environment": "normal"
        },
        "macro_bias": {
            "fed_policy": "neutral",
            "rate_expectation": "mixed",
            "fiscal_stance": "neutral",
            "usd_bias": "neutral"
        },
        "market_structure": {
            "breadth": "unknown",
            "advance_decline": "unknown",
            "participation": "unknown"
        },
        "spx_structure": {
            "key_support": [],
            "key_resistance": [],
            "upside_targets": [],
            "downside_targets": []
        },
        "volatility_state": {
            "vix_state": "unknown",
            "dealer_gamma": "unknown",
            "volatility_risk": "contained"
        },
        "daily_thesis": {
            "expected_behavior": "uncertain",
            "dominant_risk": "unknown",
            "invalidation_conditions": []
        }
    }

def get_quant_memory_template(date_str):
    return {
        "date": date_str,
        "quant_levels": {
            "support": [],
            "resistance": [],
            "reversal_zones": [],
            "extreme_levels": []
        },
        "level_roles": {
            "mean_reversion": [],
            "high_probability_reversal": [],
            "breakout_levels": []
        },
        "quant_state": {
            "directional_bias": "neutral",
            "preferred_play": "wait",
            "risk_level": "medium"
        }
    }

# Parsing Logic
def parse_market_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        text = f.read().lower()
    
    data = get_market_memory_template("YYYY-MM-DD") # Date updated later
    
    # 1. Regime/Trend
    if any(w in text for w in ["range", "chop", "sideways", "flat", "correction in time"]):
        data["regime"]["trend"] = "range"
    elif any(w in text for w in ["bullish", "uptrend", "rally", "breakout", "highs"]):
        data["regime"]["trend"] = "bullish"
    elif any(w in text for w in ["bearish", "downtrend", "selloff", "correction", "decline"]):
        data["regime"]["trend"] = "bearish"

    # 2. Levels extraction (Simple heuristic: look for 4 digit numbers near keywords)
    # This is not perfect but effective for bulk history
    supports = re.findall(r"support.*?(\d{4})", text)
    resistances = re.findall(r"resistance.*?(\d{4})", text)
    
    # Fallback: Find all 4-digit numbers between 3000 and 7000 if explicit keywords fail
    if not supports and not resistances:
        all_levels = [int(x) for x in re.findall(r"\b(4\d{3}|5\d{3}|6\d{3})\b", text)]
        # Assume lower half are supports, upper half resistances for simplicity in bulk
        if all_levels:
            mid = sum(all_levels) / len(all_levels)
            supports = [x for x in all_levels if x < mid]
            resistances = [x for x in all_levels if x >= mid]

    data["spx_structure"]["key_support"] = [int(x) for x in supports][:3] # Limit to top 3
    data["spx_structure"]["key_resistance"] = [int(x) for x in resistances][:3]
    
    # 3. Volatility
    if "contango" in text or "low vol" in text or "vix crushed" in text:
        data["volatility_state"]["vix_state"] = "low"
    elif "backwardation" in text or "high vol" in text or "vix spike" in text:
        data["volatility_state"]["vix_state"] = "high"

    return data

def parse_quant_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    data = get_quant_memory_template("YYYY-MM-DD")
    
    all_levels = []
    
    for line in lines:
        line = line.strip().lower()
        # Look for levels starting a line: "6853: 21d ema"
        match = re.match(r"^(\d{4})", line)
        if match:
            level = int(match.group(1))
            all_levels.append(level)
            
            if "reversal" in line:
                data["quant_levels"]["reversal_zones"].append(level)
                data["level_roles"]["mean_reversion"].append(level)
            
            if "pivot" in line:
                data["level_roles"]["breakout_levels"].append(level)
                
            if "support" in line: # rare in line definition, usually in block
                data["quant_levels"]["support"].append(level)

        # Look for "Support: 6810-6824" blocks
        if "support:" in line:
            nums = re.findall(r"(\d{4})", line)
            data["quant_levels"]["support"].extend([int(x) for x in nums])
            
        if "resistance:" in line:
            nums = re.findall(r"(\d{4})", line)
            data["quant_levels"]["resistance"].extend([int(x) for x in nums])

    # Categorize remaining raw levels if lists are empty
    if all_levels:
        all_levels = sorted(list(set(all_levels)))
        if not data["quant_levels"]["support"]:
            data["quant_levels"]["support"] = all_levels[:2]
        if not data["quant_levels"]["resistance"]:
            data["quant_levels"]["resistance"] = all_levels[-2:]
        if not data["quant_levels"]["extreme_levels"]:
             data["quant_levels"]["extreme_levels"] = [all_levels[0], all_levels[-1]]

    # Directional Bias logic
    pivot = next((l for l in data["level_roles"]["breakout_levels"]), None)
    if pivot:
        data["quant_state"]["preferred_play"] = "breakout"
    else:
        data["quant_state"]["preferred_play"] = "fade" # Default to fade/mean reversion

    return data

def main():
    print(f"Starting historical JSON generation in: {ROOT_DIR}")
    
    if not ROOT_DIR.exists():
        print(f"Error: Directory {ROOT_DIR} does not exist.")
        return

    processed_count = 0
    
    for date_dir in ROOT_DIR.iterdir():
        if not date_dir.is_dir():
            continue
            
        date_str = date_dir.name # "20260105"
        
        # Convert folder name to YYYY-MM-DD
        try:
            formatted_date = datetime.strptime(date_str, "%Y%m%d").strftime("%Y-%m-%d")
        except ValueError:
            print(f"Skipping invalid folder name: {date_str}")
            continue

        # --- Market Memory ---
        market_json_path = date_dir / "market_memory.json"
        daily_analysis_dir = date_dir / "DailyAnalysis"
        
        if not (SKIP_EXISTING and market_json_path.exists()):
            if daily_analysis_dir.exists():
                txt_files = list(daily_analysis_dir.glob("*.txt"))
                if txt_files:
                    try:
                        market_data = parse_market_file(txt_files[0])
                        market_data["date"] = formatted_date
                        with open(market_json_path, 'w') as f:
                            json.dump(market_data, f, indent=2)
                        print(f"[{date_str}] Generated market_memory.json")
                        processed_count += 1
                    except Exception as e:
                        print(f"[{date_str}] Error parsing market file: {e}")

        # --- Quant Memory ---
        quant_json_path = date_dir / "quant_memory.json"
        quant_updates_dir = date_dir / "QuantUpdates"
        
        if not (SKIP_EXISTING and quant_json_path.exists()):
            if quant_updates_dir.exists():
                txt_files = list(quant_updates_dir.glob("*.txt"))
                if txt_files:
                    try:
                        quant_data = parse_quant_file(txt_files[0])
                        quant_data["date"] = formatted_date
                        with open(quant_json_path, 'w') as f:
                            json.dump(quant_data, f, indent=2)
                        print(f"[{date_str}] Generated quant_memory.json")
                        processed_count += 1
                    except Exception as e:
                        print(f"[{date_str}] Error parsing quant file: {e}")

    print(f"Done. Processed/Created {processed_count} files.")

if __name__ == "__main__":
    main()