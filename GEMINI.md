# Estudio Global: Proyecto TearsDeepMind (Actualizado)

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
*   **`SubscriberController`**: Gestión dinámica de la lista de distribución de reportes.
*   **`HistoryController`**: Provee acceso a la memoria histórica del agente (análisis pasados y evolución de niveles).

### B. Capa de Negocio (Core Logic)
*   **`TearsAgentService` (El Cerebro):** Integra el LLM Gemini con lógica de reintentos y fallback. Utiliza templates dinámicos para estructurar la salida.
*   **`SubscriberService`**: Gestiona el ciclo de vida de los destinatarios de los informes.
*   **`PipelineService`**: Director de orquesta que coordina el flujo completo de datos e inteligencia.
*   **`EmailService`**: Sistema de notificaciones que consulta dinámicamente los suscriptores activos en BD.
*   **`CrawlerService` (Los Ojos):** Utiliza Selenium/RemoteWebDriver para la extracción asíncrona de datos financieros.

### C. Capa de Persistencia (Data Access)
Utiliza **JPA/Hibernate** sobre **PostgreSQL**, con un diseño de esquema segregado gestionado por **Flyway**.
*   **Schema `crawler`**: Aísla la logística de extracción (`crawler_jobs`).
*   **Schema `analysis`**: Contiene el conocimiento (`daily_analysis`, `quant_memory`) y la meta-información (`templates`).
*   **Schema `notification`**: Gestión de suscriptores (`subscribers`).
*   **Conversión Flexible:** Uso de `AttributeConverter` para mapear columnas JSON a `Map<String, Object>`.

---

## 3. Modelo de Dominio y Datos

### Inteligencia Narrativa (`DailyAnalysis`)
*   **Propósito:** Entender el *contexto* (Régimen, Sesgo Macro, Estructura SPX).

### Inteligencia Cuantitativa (`QuantMemory`)
*   **Propósito:** Definir el *precio* (Niveles Gamma, Reversión, Riesgo).

### Notificaciones (`Subscribers`)
*   **Propósito:** Gestión dinámica de la audiencia. Permite activar/desactivar receptores sin reinicio del sistema.

---

## 4. Lógica de IA y Flujo de Decisión

1.  **Ingesta Dual:** Recuperación de narrativa y datos cuantitativos.
2.  **Context Injection:** Fusión de textos en el prompt maestro.
3.  **Razonamiento Guiado:** Generación de informe Markdown (Analista Institucional).
4.  **Extracción Estructurada:** Mapeo de Markdown a JSON usando templates en BD para garantizar consistencia técnica.

---

## 5. Infraestructura y Operaciones

*   **Runtime:** Contenedores Docker (App, PostgreSQL, Selenium Standalone).
*   **Base de Datos:** PostgreSQL con versionado de esquema estricto (Flyway V1-V4).
*   **Observabilidad:** Punto de mejora detectado (pendiente añadir Actuator/Prometheus).

---

## 6. Diagnóstico y Recomendaciones (Senior Audit)
1.  **Observabilidad:** Implementar endpoints de salud y métricas.
2.  **Resiliencia:** Migrar lógica manual de reintentos de IA a Resilience4j.
3.  **Auditoría:** Crear tabla de histórico de envíos de email para trazabilidad completa.