package com.tearsdeepmind.domain.model;

import java.time.LocalDate;
import java.util.List;

public record QuantMemoryRecord(
    LocalDate date,
    List<ExtractedLevel> extracted_levels,
    DefinedZones defined_zones,
    String quant_commentary
) {
    public record ExtractedLevel(
        String raw_text,
        List<Double> numeric_values,
        String type,
        String confidence
    ) {}

    public record DefinedZones(
        List<String> support,
        List<String> resistance,
        List<String> buy_zone,
        List<String> sell_zone
    ) {}
}
