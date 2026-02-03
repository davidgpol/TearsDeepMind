package com.tearsdeepmind.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports", schema = "analysis")
public class ReportEntity {

    @Id
    private LocalDate date;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "generated_at", insertable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "source_quant_id")
    private UUID sourceQuantId;

    @Column(name = "source_macro_id")
    private UUID sourceMacroId;

    public ReportEntity() {}

    public ReportEntity(LocalDate date, String content, UUID sourceQuantId, UUID sourceMacroId) {
        this.date = date;
        this.content = content;
        this.sourceQuantId = sourceQuantId;
        this.sourceMacroId = sourceMacroId;
    }

    // Getters and Setters
    public LocalDate getDate() { return date; }
    public String getContent() { return content; }
    public UUID getSourceQuantId() { return sourceQuantId; }
    public UUID getSourceMacroId() { return sourceMacroId; }
}
