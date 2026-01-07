-- Actualización Esquema: Crawler Jobs
-- Fecha: 2025-12-31
-- PlatformAgent

CREATE TABLE IF NOT EXISTS crawler_jobs (
  job_id TEXT PRIMARY KEY,
  section TEXT NOT NULL,
  status TEXT NOT NULL,
  start_time TEXT,
  end_time TEXT,
  details TEXT -- JSON con urls pendientes, completadas y taskDetails
);
