-- V6: Create Market Data Schema and Tables for Cognitive Architecture

CREATE SCHEMA IF NOT EXISTS market_data;

-- 1. Daily Candles (The Big Picture)
-- Stores historical daily data for SPX, VIX, etc.
CREATE TABLE market_data.daily_candles (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL, -- ^GSPC, ^VIX
    date DATE NOT NULL,
    open DECIMAL(10, 2) NOT NULL,
    high DECIMAL(10, 2) NOT NULL,
    low DECIMAL(10, 2) NOT NULL,
    close DECIMAL(10, 2) NOT NULL,
    volume BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(symbol, date)
);

-- Index for fast range queries (Give me last 200 days)
CREATE INDEX idx_daily_candles_symbol_date ON market_data.daily_candles(symbol, date DESC);

-- 2. Intraday Candles (The Movie)
-- Stores 5-minute candles for detailed auditing.
-- Retention Policy: We will run a job to delete rows older than 90 days.
CREATE TABLE market_data.intraday_candles (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL, -- UTC Epoch
    open DECIMAL(10, 2) NOT NULL,
    high DECIMAL(10, 2) NOT NULL,
    low DECIMAL(10, 2) NOT NULL,
    close DECIMAL(10, 2) NOT NULL,
    volume BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(symbol, timestamp)
);

CREATE INDEX idx_intraday_candles_symbol_time ON market_data.intraday_candles(symbol, timestamp DESC);

-- 3. Technical Indicators (The Calculator)
-- Stores the computed values for a specific date to avoid re-calculation.
CREATE TABLE market_data.technical_indicators (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    date DATE NOT NULL,
    
    -- Trend Indicators
    ema_9d DECIMAL(10, 2),
    ema_21d DECIMAL(10, 2),
    sma_50d DECIMAL(10, 2),
    sma_200d DECIMAL(10, 2),
    ema_21w DECIMAL(10, 2), -- The Macro Trend
    
    -- Volatility Indicators
    atr_14d DECIMAL(10, 2),
    rsi_14d DECIMAL(10, 2),
    
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(symbol, date)
);

-- 4. Audit Logs (The Judge's Verdict)
-- Stores the performance review of the Agent's predictions.
CREATE TABLE market_data.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    quant_memory_id BIGINT, -- Logical FK to analysis.quant_memory(id)
    
    -- Boolean flags for simple stats
    direction_correct BOOLEAN,
    volatility_regime_correct BOOLEAN,
    
    -- Detailed scoring
    level_precision_score DECIMAL(5, 2), -- 0.0 to 10.0
    
    -- The Textual Verdict (to be injected into the next prompt)
    verdict_summary TEXT, 
    
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_date ON market_data.audit_logs(date DESC);
