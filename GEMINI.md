# Estudio Global: Proyecto TearsDeepMind

## 1. Visión General del Sistema
**TearsDeepMind** es una plataforma autónoma de inteligencia financiera diseñada para operar el índice **SP500**. Su núcleo es un orquestador híbrido que combina **Ingeniería de Datos tradicional** (crawling) con **Inteligencia Artificial Generativa** (Gemini LLM) para simular el proceso de toma de decisiones de un trader institucional.

El sistema transforma **datos no estructurados** (narrativa de foros, reportes macro) en **inteligencia estructurada** (señales de trading, niveles clave) persistida en una base de datos relacional.

---

## 2. Arquitectura de Software (Capas y Componentes)

El proyecto implementa una arquitectura monolítica modular basada en **Spring Boot 3.2** y **Java 21**, estructurada en capas clásicas pero con segregación de dominios.

### A. Capa de Presentación (REST API)
Expone la funcionalidad del sistema al mundo exterior y al programador de tareas.
*   **`CrawlerControllerV2`**: Gestiona la ingesta de datos brutos. Permite lanzar extracciones bajo demanda o verificar cambios.
*   **`PipelineController`**: El punto de entrada para ejecutar el ciclo de inteligencia completo (Extracción -> Análisis -> Persistencia -> Alerta).
*   **`HistoryController`**: Provee acceso a la memoria histórica del agente (análisis pasados y evolución de niveles).

### B. Capa de Negocio (Core Logic)
*   **`TearsAgentService` (El Cerebro):**
    *   Integra el LLM Gemini.
    *   Implementa **Prompt Engineering dinámico**: inyecta datos del mercado en un "System Prompt" y utiliza "Templates" almacenados en BD para forzar al LLM a responder en JSON estructurado.
    *   Maneja la resiliencia de la IA con una lógica de reintentos y fallback entre modelos.
*   **`PipelineService` (El Director):** Orquesta el flujo de trabajo, maneja excepciones globales y asegura que un fallo en el envío de emails no aborte la persistencia de los datos analizados.
*   **`CrawlerService` (Los Ojos):** Utiliza Selenium para navegar, extraer contenido y sanitizarlo. Opera de forma asíncrona para no bloquear el hilo principal.

### C. Capa de Persistencia (Data Access)
Utiliza **JPA/Hibernate** sobre **PostgreSQL**, con un diseño de esquema segregado gestionado por **Flyway**.
*   **Schema `crawler`**: Aísla la logística de extracción (`crawler_jobs`).
*   **Schema `analysis`**: Contiene el conocimiento (`daily_analysis`, `quant_memory`) y la meta-información (`templates`).
*   **Conversión Flexible:** Uso de `AttributeConverter` para mapear columnas JSON de la base de datos a objetos `Map<String, Object>` en Java, permitiendo flexibilidad en la estructura del análisis sin alterar el esquema SQL.

---

## 3. Modelo de Dominio y Datos

El modelo refleja la separación cognitiva de un trader:

### Inteligencia Narrativa (`DailyAnalysis`)
*   **Propósito:** Entender el *contexto*.
*   **Datos:** Régimen de mercado (tendencia/rango), sesgo macro (Fed, tipos), y estructura de volatilidad (VIX, Gamma).
*   **Identidad:** Único por día (`date` es PK).

### Inteligencia Cuantitativa (`QuantMemory`)
*   **Propósito:** Definir el *precio*.
*   **Datos:** Niveles matemáticos duros (soportes, resistencias), zonas de reversión y métricas de riesgo.
*   **Separación:** Se mantiene separada de la narrativa para evitar que el "sentimiento" contamine los "números".

### Meta-Conocimiento (`Templates`)
*   Una tabla clave que permite modificar la estructura de salida de la IA (qué campos JSON debe generar) editando la base de datos, sin recompilar el código Java.

---

## 4. Lógica de IA y Flujo de Decisión

El flujo de inteligencia artificial es sofisticado y sigue estos pasos:

1.  **Ingesta Dual:** El sistema recupera texto crudo de dos fuentes distintas: "DailyAnalysis" (Narrativa) y "QuantUpdates" (Datos).
2.  **Context Injection:** Ambos textos se fusionan e inyectan en el prompt `tears-agent-system.st`.
3.  **Razonamiento Guiado:** Se instruye a Gemini para actuar como "TearAgent" (un broker institucional).
4.  **Extracción Estructurada (JSON Mapping):**
    *   El sistema no pide texto libre. Pide JSONs que cumplan con los esquemas almacenados en la tabla `templates`.
    *   Usa un segundo prompt (`json-mapper.st`) para transformar el razonamiento difuso del LLM en objetos Java concretos (`DailyAnalysisDto`, `QuantMemoryDto`).

---

## 5. Infraestructura y Operaciones

*   **Runtime:** Contenedores Docker orquestados con `docker-compose`.
*   **Base de Datos:** PostgreSQL con versionado de esquema estricto (Flyway V1, V2, V3).
*   **Punto Débil Detectado:** Falta de **Observabilidad**. No hay métricas (Prometheus/Micrometer) ni endpoints de salud (`/actuator/health`), lo que dificulta el monitoreo en producción.
*   **Dependencia Crítica:** Selenium requiere un entorno gráfico o headless complejo, lo que engorda la imagen de Docker y aumenta el consumo de recursos.

---

## 6. Diagnóstico y Recomendaciones (Senior Audit)
Tras el estudio, se identifican los siguientes puntos de acción para elevar el sistema a nivel de producción bancaria:

1.  **Observabilidad:** Es imperativo añadir `spring-boot-starter-actuator` para tener métricas de salud y rendimiento.
2.  **Refactorización de Resiliencia:** Sustituir la lógica de reintentos manual en `TearsAgentService` por **Resilience4j** o **Spring Retry**.
3.  **Optimización del Crawler:** Migrar Selenium a un contenedor sidecar independiente (Rancher/Selenium Grid) para desacoplar el consumo de RAM de la lógica de negocio.
