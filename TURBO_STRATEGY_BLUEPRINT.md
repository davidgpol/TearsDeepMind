# 🏛️ Blueprint: Estrategia Turbo V2 & Motor Técnico Avanzado

**Objetivo:** Dotar a TearsDeepMind de capacidad de ejecución precisa mediante cálculo matemático de tiempos y selección inteligente de productos financieros (Turbos).

---

## 1. Nuevos Indicadores Técnicos (Java + Backfill)

Implementaremos en `TechnicalIndicatorService` el cálculo de 3 nuevos indicadores usando la librería `ta4j`. Estos datos se guardarán en `market_data.technical_indicators`.

### A. ATR (Average True Range)
*   **Periodo:** 14 días.
*   **Fórmula:** `Wilder's Smoothing` del True Range.
*   **Uso:** Determinar la **Velocidad Base** del mercado (Puntos/Hora).

### B. RSI (Relative Strength Index)
*   **Periodo:** 14 días.
*   **Uso:** Factor de **Fatiga** para ajustar el tiempo estimado.

### C. Bollinger Bands (Volatilidad Relativa)
*   **Periodo:** 20 días (SMA).
*   **Desviación:** 2.0.
*   **Uso:** Factor de **Compresión (Squeeze)** para identificar aceleraciones.

---

## 2. El Algoritmo de Tiempo (4-Factor Model)

Calcularemos la **Duración Estimada del Trade** (`EstimatedDuration`) basándonos en la física del mercado.

**Fórmula Maestra:**
`Duration = BaseTime * Factor_VIX * Factor_RSI * Factor_BB`

1.  **Velocidad Base (ATR):** `BaseTime = Distancia_Objetivo / (ATR_14d / 6.5)`.
2.  **Factor Aceleración (VIX):** Ajuste según la volatilidad actual vs media.
3.  **Factor Fatiga (RSI):** Penalización de tiempo en zonas de sobrecompra/venta.
4.  **Factor Explosión (Bollinger):** Reducción de tiempo en escenarios de Squeeze.

---

## 3. Integración con Scanner Vontobel (Java Nativo)

El sistema consulta en tiempo real los productos disponibles mediante un servicio interno.

*   **Tecnología:** `VontobelScannerService.java` utilizando **Jsoup** para la extracción del blob `__NEXT_DATA__`.
*   **Lógica de Selección (Filtrado de Seguridad):**
    1.  **Seguridad KO:** El nivel de KO del turbo debe estar estrictamente a más de **10 puntos** del Stop Loss táctico de la IA.
        *   *Long:* `KO < Stop_Loss_SPX - 10 pts`.
        *   *Short:* `KO > Stop_Loss_SPX + 10 pts`.
    2.  **Eficiencia:** Seleccionar el producto con mayor `leverage` (apalancamiento) dentro del margen de seguridad.

---

## 4. Motor de Precios (Pricing Engine)

Calcularemos el precio teórico del Turbo en los hitos clave del trade usando la fórmula de paridad:
*   `Price = |Spot_SPX - Strike| * Ratio`.

**Hitos en el Informe:**
1.  **Entrada:** Precio del Turbo en el Trigger.
2.  **Objetivo:** Precio del Turbo en el Take Profit.
3.  **Stop:** Precio del Turbo en el Stop Loss.

---

## 5. Estrategia Operativa Final (Informe Markdown)

La **Sección 7** del informe diario mostrará la tabla dinámica con:
*   **ISIN Seleccionado.**
*   **Nivel KO y Apalancamiento.**
*   **Precios Turbo para Entrada, Salida (Profit) y Stop.**
*   **Gestión Temporal:** Momento de Entrada (Madrid), Duración Estimada y Hora Límite de Cierre (Time-Stop).
