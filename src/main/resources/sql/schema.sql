-- Inicialización de TearsMind Database
-- Fecha: 2025-12-31
-- PlatformAgent

CREATE TABLE IF NOT EXISTS daily_analysis (
  date TEXT PRIMARY KEY,
  data TEXT NOT NULL -- SQLite usa TEXT para JSON
);

CREATE TABLE IF NOT EXISTS quant_memory (
  date TEXT PRIMARY KEY,
  data TEXT NOT NULL -- SQLite usa TEXT para JSON
);
