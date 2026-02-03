package com.tearsdeepmind.entity;

import com.tearsdeepmind.domain.model.MarketMemoryRecord;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_analysis", schema = "analysis")
public class DailyAnalysisEntity {

    @Id
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", nullable = false)
    private MarketMemoryRecord data;

    @Column(name = "source_document_id")
    private UUID sourceDocumentId;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "generated_at", insertable = false, updatable = false)
    private LocalDateTime generatedAt;

    public DailyAnalysisEntity() {}

    public DailyAnalysisEntity(LocalDate date, MarketMemoryRecord data, UUID sourceDocumentId, String promptVersion, String modelUsed) {
        this.date = date;
        this.data = data;
        this.sourceDocumentId = sourceDocumentId;
        this.promptVersion = promptVersion;
        this.modelUsed = modelUsed;
    }

    // Getters and Setters
    public LocalDate getDate() { return date; }
    public MarketMemoryRecord getData() { return data; }
    public UUID getSourceDocumentId() { return sourceDocumentId; }
}