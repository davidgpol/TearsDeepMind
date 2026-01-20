package com.tearsdeepmind.domain.model;

import javax.persistence.*;
import java.util.Map;

@Entity
@Table(name = "quant_memory", schema = "analysis")
public class QuantMemoryEntity {

    @Id
    @Column(name = "date", nullable = false)
    private String date;

    @Convert(converter = JsonToMapConverter.class)
    @Column(name = "data", columnDefinition = "TEXT", nullable = false)
    private Map<String, Object> data;

    public QuantMemoryEntity() {}

    public QuantMemoryEntity(String date, Map<String, Object> data) {
        this.date = date;
        this.data = data;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
