-- V1: Define initial tables in public schema
CREATE TABLE public.crawler_jobs (
    job_id VARCHAR(255) PRIMARY KEY,
    section VARCHAR(255),
    status VARCHAR(255),
    start_time VARCHAR(255),
    end_time VARCHAR(255),
    details TEXT
);

CREATE TABLE public.daily_analysis (
    date VARCHAR(255) PRIMARY KEY,
    data TEXT NOT NULL
);

CREATE TABLE public.quant_memory (
    date VARCHAR(255) PRIMARY KEY,
    data TEXT NOT NULL
);