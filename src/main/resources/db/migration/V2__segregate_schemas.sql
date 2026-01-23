-- V2: Segregate schemas and move tables
CREATE SCHEMA IF NOT EXISTS crawler;
CREATE SCHEMA IF NOT EXISTS analysis;

-- Migrate crawler_jobs to crawler schema (H2 Compatible)
CREATE TABLE crawler.crawler_jobs AS SELECT * FROM public.crawler_jobs;
DROP TABLE public.crawler_jobs;

-- Migrate daily_analysis to analysis schema (H2 Compatible)
CREATE TABLE analysis.daily_analysis AS SELECT * FROM public.daily_analysis;
DROP TABLE public.daily_analysis;

-- Migrate quant_memory to analysis schema (H2 Compatible)
CREATE TABLE analysis.quant_memory AS SELECT * FROM public.quant_memory;
DROP TABLE public.quant_memory;


