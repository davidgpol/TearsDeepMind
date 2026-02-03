-- V5: Data Lineage Architecture
-- 1. Create Ingestion Schema for Raw Documents
CREATE SCHEMA IF NOT EXISTS ingestion;

CREATE TABLE ingestion.raw_documents (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    date DATE NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('QUANT', 'MACRO')),
    content TEXT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(date, type)
);

-- 2. Evolve Analysis Schema to JSONB and Metadata
-- Migrate existing TEXT data to JSONB for Daily Analysis
ALTER TABLE analysis.daily_analysis RENAME TO daily_analysis_old;
CREATE TABLE analysis.daily_analysis (
    date DATE PRIMARY KEY,
    data JSONB NOT NULL,
    source_document_id UUID REFERENCES ingestion.raw_documents(id),
    prompt_version VARCHAR(50),
    model_used VARCHAR(50),
    generated_at TIMESTAMP DEFAULT NOW()
);

-- Migrate existing TEXT data to JSONB for Quant Memory
ALTER TABLE analysis.quant_memory RENAME TO quant_memory_old;
CREATE TABLE analysis.quant_memory (
    date DATE PRIMARY KEY,
    data JSONB NOT NULL,
    source_document_id UUID REFERENCES ingestion.raw_documents(id),
    prompt_version VARCHAR(50),
    model_used VARCHAR(50),
    generated_at TIMESTAMP DEFAULT NOW()
);

-- 3. Create Reports table for final Markdown output
CREATE TABLE analysis.reports (
    date DATE PRIMARY KEY,
    content TEXT NOT NULL,
    generated_at TIMESTAMP DEFAULT NOW(),
    source_quant_id UUID REFERENCES ingestion.raw_documents(id),
    source_macro_id UUID REFERENCES ingestion.raw_documents(id)
);

-- Cleanup old tables (they are empty due to previous purge)
DROP TABLE analysis.daily_analysis_old;
DROP TABLE analysis.quant_memory_old;
