package com.tearsdeepmind.domain.model;

import java.time.LocalDate;
import java.util.List;

public record MarketMemoryRecord(
    LocalDate date,
    String source_title,
    SentimentProfile sentiment_profile,
    StructuredPrediction structured_prediction,
    Drivers drivers,
    DailyThesis daily_thesis,
    NarrativeLevels narrative_levels
) {
    public record SentimentProfile(
        String bias,
        String conviction,
        String risk_appetite
    ) {}

    public record StructuredPrediction(
        String squeeze_status,
        String direction,
        String volatility,
        Double expected_range_top,
        Double expected_range_bottom,
        Double primary_target
    ) {}

    public record Drivers(
        String macro_economics,
        String monetary_policy,
        String geopolitics,
        String market_structure
    ) {}

    public record DailyThesis(
        String headline,
        String bull_case,
        String bear_case,
        String invalidation_criteria
    ) {}

    public record NarrativeLevels(
        List<String> spx_context,
        List<String> other_assets
    ) {}
}
