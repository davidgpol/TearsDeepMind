package com.tearsdeepmind.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quant_snapshots", schema = "analysis")
public class QuantSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "indicator_id", nullable = false)
    private String indicatorId;

    @Column(name = "indicator_value", nullable = false)
    private BigDecimal indicatorValue;

    @Column(name = "implied_signal")
    private String impliedSignal;

    @Column(name = "weight_used")
    private BigDecimal weightUsed;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public QuantSnapshotEntity() {}

    public QuantSnapshotEntity(LocalDate reportDate, String indicatorId, BigDecimal indicatorValue, String impliedSignal) {
        this.reportDate = reportDate;
        this.indicatorId = indicatorId;
        this.indicatorValue = indicatorValue;
        this.impliedSignal = impliedSignal;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public LocalDate getReportDate() { return reportDate; }
    public String getIndicatorId() { return indicatorId; }
    public BigDecimal getIndicatorValue() { return indicatorValue; }
}
