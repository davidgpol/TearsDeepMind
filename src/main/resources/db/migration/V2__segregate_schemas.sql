-- V2: Segregate schemas and move tables
CREATE SCHEMA IF NOT EXISTS crawler;
CREATE SCHEMA IF NOT EXISTS analysis;

-- Move tables to new schemas
ALTER TABLE crawler_jobs SET SCHEMA crawler;
ALTER TABLE daily_analysis SET SCHEMA analysis;
ALTER TABLE quant_memory SET SCHEMA analysis;
