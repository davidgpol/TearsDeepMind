import os
import json
import re
from datetime import datetime

def parse_quant(text, date_str):
    data = {
        "date": date_str,
        "quant_levels": {"support": [], "resistance": [], "reversal_zones": [], "extreme_levels": []},
        "level_roles": {"mean_reversion": [], "high_probability_reversal": [], "breakout_levels": [], "gamma_flip": None},
        "quant_state": {"directional_bias": "neutral", "preferred_play": "wait", "risk_level": "medium"}
    }
    
    # Extract numeric levels
    levels = re.findall(r"(\d{4})", text)
    if levels:
        levels = sorted(list(set(levels)), reverse=True)
        # Simple heuristic: top half resistance, bottom half support
        mid = len(levels) // 2
        data["quant_levels"]["resistance"] = levels[:mid]
        data["quant_levels"]["support"] = levels[mid:]

    # Key phrases
    if "gamma flip" in text.lower():
        gf = re.search(r"(\d{4}(?:-\d{4})?)\s*[:\-]?\s*gamma flip", text.lower())
        if gf: data["level_roles"]["gamma_flip"] = gf.group(1)
    
    if "high likelihood of reversal" in text.lower():
        revs = re.findall(r"(\d{4})\s*[:\-]?\s*high likelihood of reversal", text.lower())
        data["quant_levels"]["reversal_zones"] = revs
        data["level_roles"]["high_probability_reversal"] = revs

    # Bias detection
    bullish_terms = ["support holds", "upside", "bounce", "long"]
    bearish_terms = ["break lower", "downside", "short", "heavy"]
    bull_score = sum(1 for t in bullish_terms if t in text.lower())
    bear_score = sum(1 for t in bearish_terms if t in text.lower())
    
    if bull_score > bear_score: data["quant_state"]["directional_bias"] = "bullish"
    elif bear_score > bull_score: data["quant_state"]["directional_bias"] = "bearish"

    return data

def parse_macro(text, date_str):
    data = {
        "date": date_str,
        "regime": {"trend": "neutral", "volatility": "normal", "volume": "normal", "environment": "normal"},
        "macro_bias": {"fed_policy": "neutral", "rate_expectation": "on_hold", "fiscal_stance": "neutral", "usd_bias": "neutral"},
        "market_structure": {"breadth": "unknown", "advance_decline": "neutral", "participation": "unknown"},
        "spx_structure": {"key_support": [], "key_resistance": [], "upside_targets": [], "downside_targets": []},
        "volatility_state": {"vix_state": "unknown", "dealer_gamma": "unknown", "volatility_risk": "contained"},
        "daily_thesis": {"expected_behavior": "uncertain", "dominant_risk": "Unknown", "invalidation_conditions": []}
    }

    # Trend detection
    if any(x in text.lower() for x in ["bullish", "reclaimed", "rally", "all time highs"]): data["regime"]["trend"] = "bullish"
    if any(x in text.lower() for x in ["bearish", "breakdown", "sell off", "weakness"]): data["regime"]["trend"] = "bearish"
    
    # Volatility detection
    if "vix" in text.lower():
        if any(x in text.lower() for x in ["vix down", "declined", "low vix"]): data["volatility_state"]["vix_state"] = "low"
        if any(x in text.lower() for x in ["vix up", "spiked", "high vix"]): data["volatility_state"]["vix_state"] = "high"

    # Support/Resistance EMAs
    emas = re.findall(r"(\d+d\s?EMA)", text)
    data["spx_structure"]["key_support"] = list(set(emas))

    # Risk detection
    risks = ["earnings", "fed", "inflation", "cpi", "war", "tariffs", "boj"]
    found_risks = [r for r in risks if r in text.lower()]
    if found_risks: data["daily_thesis"]["dominant_risk"] = ", ".join(found_risks).title()

    return data

def process_all():
    base_dir = "/home/ubuntu/.gemini/Dev/Volumes/TearsMind"
    for date_folder in os.listdir(base_dir):
        full_path = os.path.join(base_dir, date_folder)
        if not os.path.isdir(full_path) or date_folder == "debug": continue
        
        date_iso = f"{date_folder[:4]}-{date_folder[4:6]}-{date_folder[6:]}"
        
        # Process DailyAnalysis -> market_memory
        da_dir = os.path.join(full_path, "DailyAnalysis")
        if os.path.exists(da_dir):
            for f in os.listdir(da_dir):
                if f.endswith(".txt"):
                    with open(os.path.join(da_dir, f), 'r') as file:
                        content = file.read()
                        result = parse_macro(content, date_iso)
                        with open(os.path.join(full_path, f"market_memory_{date_folder}.json"), 'w') as out:
                            json.dump(result, out, indent=2)

        # Process QuantUpdates -> quant_memory
        qu_dir = os.path.join(full_path, "QuantUpdates")
        if os.path.exists(qu_dir):
            for f in os.listdir(qu_dir):
                if f.endswith(".txt"):
                    with open(os.path.join(qu_dir, f), 'r') as file:
                        content = file.read()
                        result = parse_quant(content, date_iso)
                        with open(os.path.join(full_path, f"quant_memory_{date_folder}.json"), 'w') as out:
                            json.dump(result, out, indent=2)

if __name__ == "__main__":
    process_all()
