# 🏛️ TearAgent: Informe Táctico SPX 2026-02-23

### 1 Bias Direccional del SP500
- **Sesgo**: **NEUTRAL** (Defensive).
- **Justificación**: Aunque la narrativa de *Market Narrative* sugiere un sesgo neutral, la *Market Reality* muestra una estructura técnica debilitada: el precio (6863.51) opera por debajo de la EMA 9d (6869.31) y la SMA 50d (6894.76). La confluencia de un VIX elevado (19.92) y la incertidumbre arancelaria confirma un entorno de "risk-off" y fragilidad, donde los repuntes son oportunidades de venta mientras no se recupere la zona de pivote.

### 1.1 Predicción Estructurada (IA)
*Datos crudos del modelo para validación futura:*
- **Dirección Modelo**: **DOWN**
- **Rango Esperado**: 0.0 – 7000.0
- **Objetivo Primario**: 0.0 (Datos brutos indican proyección bajista extrema o falta de suelo definido en el modelo).

### 2 Contexto Macro y Catalizadores
- **Drivers Principales**:
  - **Incertidumbre Arancelaria**: La anulación de la tarifa "Liberation Day" (IEEPA) por la SCOTUS y el pivote de Trump hacia la Sección 122 mantienen la volatilidad.
  - **Geopolítica**: Tensiones con Irán y negociaciones pendientes para el 26 de febrero.
  - **Earnings**: Ansiedad previa a los resultados de Nvidia.
- **ANÁLISIS INTERMERCADO**:
  - **TNX (4.06)**: Tendencia bajista (bajo EMA 9d 4.10). La caída en rendimientos normalmente apoyaría al tech, pero en este contexto refleja búsqueda de refugio ante miedo macro/recesión.
  - **VIX (19.92)**: Opera en niveles de estrés (>19). La tendencia es ligeramente inferior a su EMA 9d (19.98), pero se mantiene lo suficientemente alto para indicar primas de opciones caras y riesgo de movimientos bruscos.
- **AUDITORÍA**: No hay auditoría previa disponible.

### 3 Lectura Quant del SP500 (Timing)
*Semáforo: PRECAUCIÓN / VENTA EN RESISTENCIAS*
- **Análisis**: El precio actual (6863.51) se encuentra en "tierra de nadie" bajista, atrapado debajo del Gamma Flip (6880).
- **Sinergia**: La *Market Reality* confirma la debilidad con el precio bajo la EMA 9d (6869). *Quant Data* advierte que 6905 es una resistencia difícil de romper. La expectativa matemática favorece reversiones en zonas de resistencia (fade rallies) o rupturas hacia los imanes inferiores (6841/6790).

### 4 Niveles Clave del SP500
*Mapa de Liquidez y Estructura*

| Nivel | Tipo | Probabilidad | Nota/Confluencia |
| :--- | :--- | :--- | :--- |
| 6950.0 | Resistencia / Reversal | 15% | **CONFLUENCIA**: Narrative Pivot + Quant High Reversal. Zona donde la presión cede. |
| 6938.0 | Magnet | 10% | Atrae el precio si rompe 6905. |
| 6896.0 - 6905.0 | Resistencia | 15% | Zona dura Quant. |
| 6894.76 | **TECH** | N/A | **SMA 50d**: Resistencia dinámica mayor. |
| 6880.0 | Gamma Level | 10% | **Gamma Flip**. Clave para cambio de régimen. |
| 6869.31 | **TECH** | N/A | **EMA 9d**: Resistencia inmediata de tendencia. |
| **6866.0 - 6861.0** | **ZONA DE BATALLA** | 5% | **Price Action Actual (6863.51)**. Confluencia: Nivel Quant (6866) + Narrative Overnight (6861). La sesión se define aquí. |
| 6841.0 - 6846.0 | Soporte / Nivel | 10% | Primer objetivo bajista claro. |
| 6790.0 | Soporte / Reversal | 15% | **Mean Reversal**. Zona de compra esperada por Quant. |

### 5 Estructura de Opciones & Volatilidad
- **Régimen**: **Gamma Negativa**. Al estar el precio (6863) por debajo del nivel de *Gamma Flip* (6880) proporcionado por Quant Data, los Market Makers probablemente están vendiendo en las caídas y comprando en las subidas, exacerbando la volatilidad.
- **VIX**: 19.92. Aunque la EMA 9d está ligeramente por encima, el nivel absoluto sugiere **Expansión** de rangos intradía.

### 6 Estrategias Operativas (Opciones)
*Estrategia alineada con Bias: DOWN*
- **Estrategia**: **Long Put Vertical Spread (Bear Put)**.
- **Strike Sugerido**: Comprar Put 6860 / Vender Put 6800.
- **Trigger**: Pérdida confirmada de la **ZONA DE BATALLA** (6861) o rechazo al tocar la EMA 9d (6869).
- **Stop Loss**: **POR TOQUE** en 6881 (Justo encima del Gamma Flip).

### 7 Estrategias Operativas (Turbos)
*Obediencia a Narrativa: SHORT*

**🚀 Selección Inteligente (IA + Scanner Vontobel)**

| Parámetro | Valor | Notas Tácticas |
| :--- | :--- | :--- |
| **Producto** | **Turbo SHORT SPX** | ISIN: **DE000VH54E31** (Simulado) |
| **KO (Barrera)** | **7020.00** | Situado sobre Invalidation Level (7000). |
| **Apalancamiento** | **~43x** | Riesgo Alto. Ajustado por VIX alto. |

**Plan de Ejecución (Precios Teóricos)**

1.  **Entrada (Trigger)**:
    *   **SPX Nivel:** **6863.51** (Mercado)
    *   **Turbo Precio:** **~1.56€** (Calculado: (7020 - 6863.51) * 0.01)

2.  **Salida (Take Profit)**:
    *   **SPX Nivel:** **6790.00** (Ajuste Táctico a Soporte Quant)
    *   **Turbo Precio:** **~2.30€** (Calculado: (7020 - 6790) * 0.01)
    *   *Nota: Aunque el target primario es 0.0, tomamos beneficios en estructura.*

3.  **Stop Loss (Emergencia)**:
    *   **SPX Nivel:** **6881.00** (Toque Gamma Flip)
    *   **Turbo Precio:** **~1.39€**

**⏱️ Gestión Temporal (Time-Stop Madrid)**
*   **Duración Estimada**: **Intradía puro**.
*   **Hora Límite**: **21:50 CET**.
*   **Instrucción**: Si el precio recupera la EMA 9d (6869), cerrar posición manualmente.

### 8 Plan de Trading (Execution)
- **Objetivo Final**: El target del modelo es 0.0, pero tácticamente buscamos la liquidez en **6790** (Quant Reversal) y **6775**.
- **Instrucción Condicional**:
  - "Si SPX pierde **6861** (Narrative Overnight Low), activar cortos agresivos hacia **6841**. Si rompe 6841, dejar correr hacia **6790**."
  - "Cualquier rebote hacia **6880-6894** (Gamma Flip / SMA 50d) es oportunidad de venta si VIX > 19."

### 9 Gestión de Riesgo
- **Tamaño de Posición**: **REDUCIDO (50% del habitual)**. VIX en 19.92 y Gamma Negativa implican riesgo de "whipsaw" (movimientos violentos en ambas direcciones).
- **Convicción**: **MEDIA**. La alineación técnica (precio < medias) y narrativa es bajista, pero estamos operando dentro de una "Zona de Batalla" (6866) sin resolución clara al momento del informe.

### 10 Conclusión Final de TearAgent
La oportunidad con mejor asimetría es el **SHORT** tras la confirmación de la ruptura de **6861** o en un rechazo táctico en **6880** (Gamma Flip). La estructura de mercado es frágil; la defensa de los 6860 es crítica para los alcistas. Si cede, la magnetismo hacia 6790 es alto. Mantener stops ajustados por toque en 6881.
