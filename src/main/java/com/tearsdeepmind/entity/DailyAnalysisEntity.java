package com.tearsdeepmind.entity;

import jakarta.persistence.*;
import java.util.Map;

@Entity
@Table(name = "daily_analysis", schema = "analysis")
public class DailyAnalysisEntity {

    @Id
    @Column(name = "date", nullable = false)
    private String date;

    @Convert(converter = JsonToMapConverter.class)
    @Column(name = "data", columnDefinition = "TEXT", nullable = false)
    private Map<String, Object> data;

    public DailyAnalysisEntity() {}

    public DailyAnalysisEntity(String date, Map<String, Object> data) {
        this.date = date;
        this.data = data;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
