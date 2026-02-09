# Estudio Arquitectónico Integral: TearsDeepMind v2.1

**Fecha de Actualización:** 05 Febrero 2026
**Versión:** 2.1 (Data Lineage & Robustness)

## 1. Definición del Sistema
**TearsDeepMind** es una plataforma autónoma de inteligencia de mercados diseñada para operar el índice S&P 500. A diferencia de un bot de trading algorítmico tradicional basado en indicadores técnicos, TearsDeepMind simula el proceso cognitivo de un analista institucional, combinando:
1.  **Extracción de Datos No Estructurados:** Lectura de narrativa macroeconómica y foros especializados.
2.  **Modelado Cuantitativo:** Ingesta de niveles de volatilidad (VIX), Gamma Exposure (GEX) y Dark Pools.
3.  **Razonamiento LLM:** Uso de Gemini Pro para sintetizar una tesis de inversión diaria.
4.  **Linaje de Datos (Data Lineage):** Trazabilidad completa desde la fuente original hasta la señal final.

---

## 2. Arquitectura de Software (Monolito Modular)

El sistema está construido sobre **Spring Boot 3.2** y **Java 21**, utilizando características modernas como *Virtual Threads* para alta concurrencia en I/O.

### A. Capa de Ingesta (The Senses)
Esta capa es responsable de interactuar con el mundo exterior y "traer" la información al sistema de forma inmutable.

*   **`CrawlerService`**: Motor de extracción web basado en Selenium/RemoteWebDriver.
    *   *Arquitectura:* Usa `newVirtualThreadPerTaskExecutor` para lanzar múltiples navegadores en paralelo sin bloquear hilos del SO.
    *   *Resiliencia:* Implementa esperas explícitas y reintentos (aunque con selectores hardcoded).
*   **`IngestionService`**: Gatekeeper de la base de datos.
    *   *Función:* Recibe JSONs crudos, calcula un `checksum` (SHA-256) para evitar duplicados y los persiste en la tabla `ingestion.raw_documents`.
    *   *Inmutabilidad:* Una vez escrito, un `RawDocumentEntity` nunca se modifica. Es la "Fuente de la Verdad".

### B. Capa de Orquestación (The Brain)
Coordina el flujo de datos para producir inteligencia.

*   **`PipelineService`**: El director de orquesta.
    *   *Flujo V5:* 
        1.  Verifica existencia de documentos crudos para la fecha `T`.
        2.  Si faltan, dispara el `CrawlerService` de forma asíncrona y espera (`CompletableFuture.allOf`).
        3.  Recupera los documentos crudos de `ingestion`.
        4.  Invoca a `TearsAgentService`.
        5.  Persiste el resultado enlazando el `analysis` con el `raw_document` (Foreign Key lógica).
*   **`TearsAgentService`**: Cliente del LLM.
    *   *Lógica:* Construye prompts dinámicos inyectando el contenido de los documentos crudos.
    *   *Reliability:* Implementa lógica de fallback y reintentos manuales para manejar "Overloaded" exceptions de la API de Gemini.

### C. Capa de Dominio y Persistencia (The Memory)
La base de datos PostgreSQL está estrictamente segregada por esquemas (DDD en SQL), gestionada por **Flyway**.

1.  **Schema `crawler`**:
    *   `crawler_jobs`: Logística de ejecuciones, estados de éxito/fracaso.
2.  **Schema `ingestion` (NUEVO V5)**:
    *   `raw_documents`: Almacén de blobs JSON. Campos: `id`, `checksum`, `content`, `source_type` (MACRO/QUANT), `extraction_date`.
3.  **Schema `analysis`**:
    *   `daily_analysis`: La narrativa procesada. Columna `source_document_id` apunta a `raw_documents`.
    *   `quant_memory`: Los números procesados. Columna `source_document_id` apunta a `raw_documents`.
    *   `templates`: Prompts del sistema versionados.
4.  **Schema `notification`**:
    *   `subscribers`: Lista de distribución dinámica con estados (ACTIVE/INACTIVE).
    *   `reports`: Histórico de informes generados (Markdown/PDF) listos para envío.

### D. Capa de Entrega (The Voice)
*   **`EmailService`**:
    *   Construye correos MIME Multipart.
    *   Cuerpo: Resumen ejecutivo (Bias + Headline).
    *   Adjunto: Informe completo `.md` (previamente guardado en `ReportEntity`).
*   **`SubscriberService`**: Permite la gestión "en caliente" de quién recibe los correos a través de una API REST.

---

## 3. Flujos de Datos Críticos

### Flujo Diario (Pipeline)
1.  **Trigger:** Cron (09:00 AM) o REST `/api/v2/pipeline/run/{date}`.
2.  **Check:** ¿Existen datos en `ingestion.raw_documents` para hoy?
    *   *NO:* `CrawlerService` despierta -> Selenium extrae -> `IngestionService` guarda (Checksum OK).
    *   *SI:* Se procede.
3.  **Reasoning:** `PipelineService` lee Raw -> `TearsAgent` piensa -> Genera `DailyAnalysis` y `QuantMemory`.
4.  **Reporting:** Se genera un `ReportEntity` (Markdown consolidado) enlazando ambos análisis.
5.  **Delivery:** `EmailService` lee `ReportEntity` y lo envía a todos los `subscribers` activos.

### Flujo de Backfill (Histórico)
*   **`HistoricalBackfillService`**:
    *   Itera sobre carpetas locales de una estructura de archivos legacy.
    *   Inyecta los JSONs antiguos en `ingestion.raw_documents`.
    *   Dispara el `PipelineService` para re-generar la inteligencia y poblar la base de datos con datos históricos limpios.

---

## 4. Tecnologías Clave

*   **Lenguaje:** Java 21 (LTS).
*   **Framework:** Spring Boot 3.2.x.
*   **BD:** PostgreSQL 16 (con Flyway).
*   **IA:** Google Gemini Pro 1.5.
*   **Extracción:** Selenium Grid (Standalone Chrome).
*   **Contenedores:** Docker & Docker Compose.
*   **Testing:** JUnit 5 (Actualmente reducidos en integración).

---

## 5. Estado Actual (Snapshot)
*   **Integridad:** Alta. El sistema de checksums evita duplicados.
*   **Trazabilidad:** Completa. Es posible auditar por qué el agente dijo "BULLISH" mirando el documento crudo exacto.
*   **Resiliencia:** Media. El Crawler es el punto único de fallo (selectores hardcoded). El Pipeline tiene reintentos básicos.
*   **Operatividad:** Alta. API REST completa para gestión y backfill.

Este documento sirve como la "Fuente de la Verdad" arquitectónica para el desarrollo continuo.
