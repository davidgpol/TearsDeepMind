-- V3: Create templates table and insert initial data
CREATE TABLE analysis.templates (
    name VARCHAR(255) PRIMARY KEY,
    content TEXT NOT NULL
);

INSERT INTO analysis.templates (name, content) VALUES
('market_memory', '{
  "date": "YYYY-MM-DD",
  "regime": {
    "trend": "bullish | bearish | neutral | range",
    "volatility": "compressed | normal | expanded",
    "volume": "thin | normal | heavy",
    "environment": "holiday | normal | event_driven | crisis"
  },
  "macro_bias": {
    "fed_policy": "accommodative | neutral | restrictive",
    "rate_expectation": "cuts | hikes | on_hold | mixed",
    "fiscal_stance": "expansionary | neutral | restrictive",
    "usd_bias": "strong | neutral | weak"
  },
  "market_structure": {
    "breadth": "expanding | flat | contracting | ath | unknown",
    "advance_decline": "confirming | diverging | neutral | unknown",
    "participation": "broad | narrow | mixed | unknown"
  },
  "spx_structure": {
    "key_support": [],
    "key_resistance": [],
    "upside_targets": [],
    "downside_targets": []
  },
  "volatility_state": {
    "vix_state": "low | mid | high | unknown",
    "dealer_gamma": "positive | neutral | negative | thin | unknown",
    "volatility_risk": "contained | elevated | extreme | unknown"
  },
  "daily_thesis": {
    "expected_behavior": "continuation | mean_reversion | range | volatile | uncertain",
    "dominant_risk": "",
    "invalidation_conditions": []
  }
}'),
('quant_memory', '{
  "date": "YYYY-MM-DD",
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
    "directional_bias": "bullish | bearish | neutral | mixed",
    "preferred_play": "fade | breakout | wait",
    "risk_level": "low | medium | high"
  }
}');
