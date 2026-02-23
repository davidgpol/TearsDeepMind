# 🏛️ Blueprint: Estrategia Turbo V2 & Motor Técnico Avanzado

**Objetivo:** Dotar a TearsDeepMind de capacidad de ejecución precisa mediante cálculo matemático de tiempos y selección inteligente de productos financieros (Turbos).

---

## 1. Nuevos Indicadores Técnicos (Java + Backfill)

Implementaremos en `TechnicalIndicatorService` el cálculo de 3 nuevos indicadores usando la librería `ta4j`. Estos datos se guardarán en `market_data.technical_indicators`.

### A. ATR (Average True Range)
*   **Periodo:** 14 días.
*   **Fórmula:** `Wilder's Smoothing` del True Range.
    *   `TR = Max(High-Low, |High-Close_Prev|, |Low-Close_Prev|)`
    *   `ATR = (ATR_Prev * 13 + TR_Current) / 14`
*   **Uso:** Determinar la **Velocidad Base** del mercado (Puntos/Hora).

### B. RSI (Relative Strength Index)
*   **Periodo:** 14 días.
*   **Fórmula:** `100 - (100 / (1 + RS))` donde `RS = AvgGain / AvgLoss`.
*   **Uso:** Factor de **Fatiga**.
    *   Si RSI > 70 (Sobrecompra) y vamos LONG -> El movimiento será más lento/errático.
    *   Si RSI < 30 (Sobreventa) y vamos SHORT -> Idem.

### C. Bollinger Bands (Volatilidad Relativa)
*   **Periodo:** 20 días (SMA).
*   **Desviación:** 2.0.
*   **Fórmula:** `Middle = SMA(20)`, `Upper = Middle + (2 * StdDev)`, `Lower = Middle - (2 * StdDev)`.
*   **Uso:** Factor de **Compresión (Squeeze)**.
    *   `BandWidth = (Upper - Lower) / Middle`.
    *   Si `BandWidth` < 5% (Squeeze) -> Esperamos **Aceleración** explosiva.

---

## 2. El Algoritmo de Tiempo (4-Factor Model)

Calcularemos la **Duración Estimada del Trade** (`EstimatedDuration`) basándonos en la física del mercado.

**Fórmula Maestra:**
`Duration = BaseTime * Factor_VIX * Factor_RSI * Factor_BB`

1.  **Velocidad Base (ATR):**
    *   `Speed_Points_Hour = ATR_14d / 6.5` (Sesión US estándar).
    *   `BaseTime = Distancia_Objetivo / Speed_Points_Hour`.

2.  **Factor Aceleración (VIX):**
    *   `VIX_Ratio = VIX_Current / VIX_EMA_21d`.
    *   `Factor_VIX = 1.0 / VIX_Ratio` (A mayor VIX, menor tiempo).

3.  **Factor Fatiga (RSI):**
    *   Si `(Direction == LONG && RSI > 70)` O `(Direction == SHORT && RSI < 30)`:
        *   `Factor_RSI = 1.2` (+20% Tiempo).
    *   Else: `Factor_RSI = 1.0`.

4.  **Factor Explosión (Bollinger):**
    *   Si `BB_Width < 0.05` (Squeeze):
        *   `Factor_BB = 0.8` (-20% Tiempo).
    *   Else: `Factor_BB = 1.0`.

5.  **Resultado Final:**
    *   `Hora_Limite_Madrid = Hora_Actual + Duration`.

---

## 3. Integración con Scanner ISIN (Java <-> Python)

Usaremos el script `sp500_scanner.py` existente como motor de búsqueda.

*   **Invocación:** `ProcessBuilder` desde Java.
*   **Comando:** `python3 docs/PoC/ISIN/sp500_scanner.py --underlying SPX --direction {LONG/SHORT} --json`
*   **Filtrado Inteligente (Java):**
    1.  **Seguridad KO:** El nivel de KO del turbo debe estar *más allá* del Stop Loss calculado por la IA.
        *   *Long:* `KO < Stop_Loss_SPX - 10 pts`.
        *   *Short:* `KO > Stop_Loss_SPX + 10 pts`.
    2.  **Apalancamiento Máximo:** De los seguros, elegir el de mayor `leverage`.

---

## 4. Motor de Precios (Pricing Engine)

Calcularemos el precio teórico del Turbo en los hitos clave del trade.

*   **Fórmulas de Paridad:**
    *   `Price_Long = (Spot_SPX - Strike) * Ratio`
    *   `Price_Short = (Strike - Spot_SPX) * Ratio`
*   **Hitos Calculados:**
    1.  **Entrada:** Precio del Turbo cuando SPX toca el nivel de entrada (Trigger).
    2.  **Objetivo:** Precio del Turbo cuando SPX toca el Target (Take Profit).
    3.  **Stop:** Precio del Turbo cuando SPX toca el Stop Loss (Salida).

---

## 5. Estrategia Operativa Final (Informe Markdown)

La **Sección 7** del informe diario evolucionará para mostrar esta tabla dinámica:

### 7 Estrategias Operativas (Turbos)
*Obediencia a Narrativa: {DIRECTION}*

**🚀 Selección Inteligente (IA + Scanner Vontobel)**

| Parámetro | Valor | Notas Tácticas |
| :--- | :--- | :--- |
| **Producto** | **Turbo {DIRECTION} SPX** | ISIN: **{ISIN}** |
| **KO (Barrera)** | **{KO_LEVEL}** | Riesgo de liquidación total. |
| **Apalancamiento** | **{LEVERAGE}x** | Alto Riesgo. |

**Plan de Ejecución (Precios Teóricos)**

1.  **Entrada (Trigger)**:
    *   **SPX Nivel:** **{ENTRY_SPX}** (Ruptura EMA 9d).
    *   **Turbo Precio:** **~{ENTRY_TURBO}€**.
    *   *Acción:* Comprar a mercado.

2.  **Salida (Take Profit)**:
    *   **SPX Nivel:** **{TARGET_SPX}** (Resistencia Mayor).
    *   **Turbo Precio:** **~{TARGET_TURBO}€** (+{ROI}%).
    *   *Acción:* Vender el 100% de la posición.

3.  **Stop Loss (Emergencia)**:
    *   **SPX Nivel:** **{STOP_SPX}** (Soporte Quant).
    *   **Turbo Precio:** **~{STOP_TURBO}€**.
    *   *Acción:* VENDER INMEDIATAMENTE si toca.

**⏱️ Gestión Temporal (Time-Stop Madrid)**
*   **Momento de Entrada**: **{ENTRY_TIME_MADRID} CET**. (Ventana óptima tras apertura US o ruptura confirmada).
*   **Duración Estimada**: **~{DURATION}** (Calculado por ATR/VIX/RSI/BB).
*   **Hora Límite**: **{EXIT_TIME_MADRID} CET**.
*   **Instrucción**: Si a las {EXIT_TIME_MADRID} no se alcanza el objetivo (**{TARGET_TURBO}€**), CERRAR LA POSICIÓN a mercado.
