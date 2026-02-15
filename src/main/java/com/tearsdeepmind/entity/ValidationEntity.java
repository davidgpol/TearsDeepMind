package com.tearsdeepmind.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "validations", schema = "analysis")
public class ValidationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "prediction_id", nullable = false)
    private UUID predictionId;

    @Column(name = "score", nullable = false)
    private BigDecimal score;

    @Column(name = "error_margin")
    private BigDecimal errorMargin;

    @Column(name = "verdict")
    private String verdict;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public ValidationEntity() {}

    public ValidationEntity(UUID predictionId, BigDecimal score, BigDecimal errorMargin, String verdict, String notes) {
        this.predictionId = predictionId;
        this.score = score;
        this.errorMargin = errorMargin;
        this.verdict = verdict;
        this.notes = notes;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public UUID getPredictionId() { return predictionId; }
    public BigDecimal getScore() { return score; }
    public String getVerdict() { return verdict; }
}
