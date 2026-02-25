# Estudio Arquitectónico Integral: TearsDeepMind v2.1 (V5 Data Lineage)

## 1. Visión General del Sistema
**TearsDeepMind** es una plataforma autónoma de inteligencia de mercados para el S&P 500. Simula el razonamiento de un trader institucional mediante un pipeline híbrido:
1.  **Ingeniería de Datos:** Crawling asíncrono y almacenamiento inmutable.
2.  **Inteligencia Artificial:** Análisis de contexto y precio con Gemini 1.5 Pro.
3.  **Trazabilidad (Lineage):** Vinculación estricta entre datos crudos y decisiones finales.

---

## 2. Arquitectura de Software (Capas y Dominios)

Monolito modular en **Spring Boot 3.2 (Java 21)** con segregación estricta de esquemas en PostgreSQL.

### A. Capa de Ingesta (The Senses)
*   **`CrawlerService`**: Usa **Virtual Threads** para orquestar navegadores Selenium en paralelo. Extrae Macro y Quant.
*   **`IngestionService`**: Gatekeeper de integridad. Calcula SHA-256 de los datos entrantes y los persiste en `ingestion.raw_documents`. Es la **Fuente de la Verdad Inmutable**.

### B. Capa de Negocio (Core Logic)
*   **`PipelineService`**: Orquestador central. Sincroniza la disponibilidad de datos crudos antes de disparar el análisis.
*   **`TearsAgentService`**: Cliente de IA. Inyecta el contenido de `raw_documents` en prompts dinámicos. Gestiona reintentos y fallback.
*   **`HistoricalBackfillService`**: Permite la re-ingesta masiva de datos históricos desde el sistema de archivos legado hacia la nueva arquitectura de lineaje.

### C. Capa de Persistencia (Data Access)
Base de datos PostgreSQL segregada por esquemas (DDD):
*   **Schema `ingestion`**: Datos crudos (`raw_documents`). Inmutable.
*   **Schema `analysis`**: Inteligencia procesada (`daily_analysis`, `quant_memory`). Contiene `source_document_id` (FK lógica) para trazabilidad.
*   **Schema `notification`**: Gestión de audiencia (`subscribers`) e histórico de envíos (`reports`).
*   **Schema `crawler`**: Logística operativa (`crawler_jobs`).

### D. Capa de Entrega (The Voice)
*   **`EmailService`**: Genera correos MIME Multipart con el informe consolidado en Markdown como adjunto.
*   **`SubscriberService`**: API para gestión dinámica de suscriptores (Active/Inactive).

---

## 3. Flujo de Datos (The Lineage)

El sistema garantiza que cada conclusión analítica pueda rastrearse hasta su fuente original:
`Web` -> `Crawler` -> `RawDocument (Ingestion)` -> `AI Processing` -> `Analysis Entity` -> `Final Report`

1.  **Ingesta:** Se descarga el dato y se firma (Checksum). Se guarda en `ingestion`.
2.  **Análisis:** El LLM lee de `ingestion`. El resultado se guarda en `analysis` referenciando el ID del documento de ingestión.
3.  **Reporte:** Se consolida la información en un `ReportEntity` listo para envío.

---

## 4. Estado Actual (V2.2 - Full Spectrum Intelligence)
*   **Inteligencia Intermercado:** Sincronización nativa de **^VIX** (Volatilidad) y **^TNX** (Bono 10Y). El sistema cruza tendencias de precios con regímenes de volatilidad y tasas para definir el sesgo.
*   **Motor de Probabilidad:** El análisis Quant normaliza matemáticamente las probabilidades de los niveles clave (Suma = 100%).
*   **Rigor Operativo:** Los informes obedecen estrictamente a la predicción estructurada (Dirección/Objetivo) y usan Stops "Por Toque".
*   **Infraestructura:** Docker Compose con Timeouts aumentados (5min) para soportar razonamiento profundo. Base de Datos reforzada (V8).
*   **Gestión de Cambios:** Flyway (V1-V8).
*   **Observabilidad:** Logging estándar.

---

## 5. Estrategia Turbo V2 & Motor Técnico (V2.3 - Feb 2026)

Esta actualización dota al sistema de capacidad de ejecución precisa mediante cálculo matemático y selección de productos.

### A. Motor Técnico Avanzado (Resurrección de Indicadores)
*   **Nuevos Indicadores:** Implementación de **ATR(14)**, **RSI(14)** y **Bollinger Bands(20,2)** en `TechnicalIndicatorService` usando `ta4j`.
*   **Corrección Histórica:** Solución del bug "Future Leak" que usaba siempre la última vela. Backfill completo (~500 días) vía endpoint `/recalculate-indicators`.
*   **Persistencia:** Datos almacenados en `market_data.technical_indicators` para análisis histórico.

### B. Scanner de Productos (Integración Java Nativa)
*   **Migración:** Portado del script `sp500_scanner.py` a Java (`VontobelScannerService.java`) usando **Jsoup** para evitar dependencias de Python/Docker.
*   **Lógica:** Extrae en tiempo real el blob JSON (`__NEXT_DATA__`) de Vontobel para encontrar ISINs, precios y barreras.

### C. Pricing & Time Engine (Algoritmos Operativos)
*   **Motor de Precios:** Calcula precios teóricos del Turbo en los niveles de Entrada, Objetivo y Stop usando la fórmula de paridad.
*   **Algoritmo de Tiempo (4 Factores):** Estima la duración del trade basándose en la física del mercado:
    *   `Duration = (Distancia / Velocidad_ATR) * Aceleración_VIX * Fatiga_RSI * Squeeze_BB`.
*   **Time-Stop:** El informe incluye una **Hora Límite (Madrid)** para cerrar la posición si el objetivo no se alcanza, basada en la volatilidad real.

---