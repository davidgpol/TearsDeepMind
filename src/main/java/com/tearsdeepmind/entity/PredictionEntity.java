package com.tearsdeepmind.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "predictions", schema = "analysis")
public class PredictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private Map<String, Object> payload;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public PredictionEntity() {}

    public PredictionEntity(LocalDate reportDate, LocalDate date, Map<String, Object> payload) {
        this.reportDate = reportDate;
        this.date = date;
        this.payload = payload;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public LocalDate getReportDate() { return reportDate; }
    public LocalDate getDate() { return date; }
    public Map<String, Object> getPayload() { return payload; }
}
