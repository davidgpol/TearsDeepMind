package com.tearsdeepmind.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "raw_documents", schema = "ingestion")
public class RawDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String type; // QUANT | MACRO

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private String checksum;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public RawDocumentEntity() {}

    public RawDocumentEntity(LocalDate date, String type, String content, String checksum) {
        this.date = date;
        this.type = type;
        this.content = content;
        this.checksum = checksum;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public String getChecksum() { return checksum; }
}
