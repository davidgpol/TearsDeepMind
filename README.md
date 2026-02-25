# TearsDeepMind v2.5: Autonomous Trading Intelligence Platform

**TearsDeepMind** is a Spring Boot 3 (Java 21) application that simulates the complete workflow of an institutional trading desk for the S&P 500 index. It operates as a fully autonomous, end-to-end pipeline that ingests raw market data, applies an advanced layer of AI-driven intelligence, and produces actionable trading strategies for Options and Turbo Warrants.

---

## Core Pipeline (The Workflow)

The system executes a daily pipeline orchestrated by `PipelineService.java`, which follows three main stages:

1.  **INGEST (The Senses):**
    *   Asynchronously crawls and scrapes raw market narrative reports (Macro) and quantitative trading levels (Quant).
    *   Persists this data immutably in a PostgreSQL database (`ingestion.raw_documents`) as the "source of truth".

2.  **ANALYZE (The Brain):**
    *   **Technical Engine:** Calculates a full suite of technical indicators (ATR, RSI, Bollinger Bands, EMAs) using `ta4j`. This provides a mathematical "market reality" check.
    *   **AI Intelligence:** Injects the raw text and the calculated technical indicators into a series of prompts for **Google Gemini 1.5 Pro**. The AI's task is to generate a structured `DailyAnalysisEntity` that includes a directional bias (`UP`, `DOWN`, `FLAT`), key price levels, and a market thesis.

3.  **STRATEGIZE (The Voice):**
    *   **Vontobel Scanner:** A native Java (Jsoup) scanner connects in real-time to Vontobel's product database, filtering for Turbo Warrants on the SPX that are explicitly available on **Trade Republic**.
    *   **Dual-Mode Strategy Engine:**
        *   **Trend Mode (`UP`/`DOWN`):** Selects the optimal Turbo to follow the predicted trend.
        *   **Range Mode (`FLAT`):** If the market is neutral, it activates a "Mean Reversion" protocol, recommending tactical trades against the edges of the predicted range with enhanced safety buffers.
    *   **Pricing & Time Engine:**
        *   Calculates precise, theoretical entry, take-profit, and stop-loss prices for the selected Turbo.
        *   Uses a **4-Factor Model** (ATR, VIX, RSI, Bollinger Width) to estimate the trade's duration and provide a strict **Time-Stop** in Madrid time (CET).
    *   **Report Synthesis:** Generates a final, comprehensive Markdown report that fuses the AI's narrative with the mathematically precise trading strategy, ready for execution.

---

## How to Run

The entire platform is containerized using Docker Compose.

```bash
docker compose up --build
```

The primary endpoint to trigger a run for a specific day is:
`POST /api/v2/pipeline/run/{YYYY-MM-DD}`
