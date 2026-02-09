package com.tearsdeepmind.dto.yahoo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YahooChartResponse(Chart chart) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chart(List<Result> result, Object error) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(Meta meta, List<Long> timestamp, Indicators indicators) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(String symbol, String currency, String exchangeName, double regularMarketPrice, long gmtoffset) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Indicators(List<Quote> quote) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Quote(List<Double> open, List<Double> high, List<Double> low, List<Double> close, List<Long> volume) {}
}
