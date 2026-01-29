package com.tearsdeepmind.dto;

import java.util.Map;

public record DailyAnalysisDto(String date, Map<String, Object> data) {
}
