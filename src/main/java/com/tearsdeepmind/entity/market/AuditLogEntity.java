package com.tearsdeepmind.entity.market;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", schema = "market_data")
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "quant_memory_id")
    private Long quantMemoryId;

    @Column(name = "direction_correct")
    private Boolean directionCorrect;

    @Column(name = "volatility_regime_correct")
    private Boolean volatilityRegimeCorrect;

    @Column(name = "level_precision_score", precision = 5, scale = 2)
    private BigDecimal levelPrecisionScore;

    @Column(name = "verdict_summary")
    private String verdictSummary;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuditLogEntity() {}

    // Getters and Setters
    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Long getQuantMemoryId() { return quantMemoryId; }
    public void setQuantMemoryId(Long quantMemoryId) { this.quantMemoryId = quantMemoryId; }
    public Boolean getDirectionCorrect() { return directionCorrect; }
    public void setDirectionCorrect(Boolean directionCorrect) { this.directionCorrect = directionCorrect; }
    public Boolean getVolatilityRegimeCorrect() { return volatilityRegimeCorrect; }
    public void setVolatilityRegimeCorrect(Boolean volatilityRegimeCorrect) { this.volatilityRegimeCorrect = volatilityRegimeCorrect; }
    public BigDecimal getLevelPrecisionScore() { return levelPrecisionScore; }
    public void setLevelPrecisionScore(BigDecimal levelPrecisionScore) { this.levelPrecisionScore = levelPrecisionScore; }
    public String getVerdictSummary() { return verdictSummary; }
    public void setVerdictSummary(String verdictSummary) { this.verdictSummary = verdictSummary; }
}
