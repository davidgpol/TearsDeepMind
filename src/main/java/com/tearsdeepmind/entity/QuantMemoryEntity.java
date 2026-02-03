package com.tearsdeepmind.entity;

import com.tearsdeepmind.domain.model.QuantMemoryRecord;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quant_memory", schema = "analysis")
public class QuantMemoryEntity {

    @Id
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", nullable = false)
    private QuantMemoryRecord data;

    @Column(name = "source_document_id")
    private UUID sourceDocumentId;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "generated_at", insertable = false, updatable = false)
    private LocalDateTime generatedAt;

    public QuantMemoryEntity() {}

    public QuantMemoryEntity(LocalDate date, QuantMemoryRecord data, UUID sourceDocumentId, String promptVersion, String modelUsed) {
        this.date = date;
        this.data = data;
        this.sourceDocumentId = sourceDocumentId;
        this.promptVersion = promptVersion;
        this.modelUsed = modelUsed;
    }

    // Getters and Setters
    public LocalDate getDate() { return date; }
    public QuantMemoryRecord getData() { return data; }
    public UUID getSourceDocumentId() { return sourceDocumentId; }
}