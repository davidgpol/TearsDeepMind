# 🏛️ TearAgent: Informe Táctico SPX 2026-02-23

### 1 Bias Direccional del SP500
- **Sesgo**: **NEUTRAL** (Bias: "neutral", Conviction: "medium").
- **Justificación**: Aunque el perfil de sentimiento es neutral, la **MARKET REALITY** impone una postura defensiva. El SPX (6863.51) ha cerrado por debajo de su tendencia inmediata (EMA 9d: 6869.31) y de la media institucional de corto plazo (SMA 50d: 6894.76). La confluencia de un VIX elevado (19.92) por encima de su media (18.96) y un precio bajo presión vendedora sugiere fragilidad estructural. El TNX en 4.06 (bajo su EMA 9d) no está proporcionando alivio suficiente a la renta variable.

### 1.1 Predicción Estructurada (IA)
*Datos crudos del modelo para validación futura:*
- **Dirección Modelo**: **DOWN**
- **Rango Esperado**: 0.0 - 7000.0
- **Objetivo Primario**: **6861.0**

### 2 Contexto Macro y Catalizadores
- **Drivers Principales**: La narrativa está dominada por la incertidumbre arancelaria (pivote a la Sección 122) y la ansiedad pre-earnings (Nvidia). Geopolíticamente, la tensión con Irán mantiene el riesgo de cola activo. La debilidad se espera "front-loaded" (lunes-miércoles).
- **ANÁLISIS INTERMERCADO**:
    - **VIX (19.92)**: Opera en zona de "Ansiedad" (>18.96). Aunque comprimió ligeramente respecto a su EMA 9d (19.98), el nivel absoluto indica una demanda de cobertura alta.
    - **TNX (4.06)**: La caída en los rendimientos sugiere flujo hacia seguridad (bonds), lo cual valida el tono defensivo del mercado de acciones.
- **AUDITORÍA**: *No hay auditoría previa disponible.*

### 3 Lectura Quant del SP500 (Timing)
*Semáforo: PRECAUCIÓN / BUSCAR REVERSIONES*
- **Análisis**: El comentario Quant destaca que el foco está en zonas de Soporte/Resistencia para reversiones intradía. El nivel **6905** es una resistencia dura.
- **SINERGIA**: El precio actual (6863.51) está atrapado debajo de la EMA 9d (6869.31). Según Quant, el nivel **6866** es un nivel intermedio. El hecho de estar operando justo debajo de 6866 y la EMA 9d confirma que el momentum bajista tiene el control inmediato, pero estamos peligrosamente cerca de una zona de soporte potencial en 6861 (Narrative) y 6841 (Quant).

### 4 Niveles Clave del SP500
*Mapa de Operaciones Consolidadas*

| Nivel | Tipo | Probabilidad | Nota/Confluencia |
| :--- | :--- | :--- | :--- |
| **7000.0** | MACRO | - | Full Release / Invalidation Level |
| **6950.0** | REVERSAL | 15% | High likelihood of reversal / Pressure eases |
| **6938.0** | MAGNET | 10% | Target magnético si rompe 6905 |
| **6896-6905** | RESISTANCE | 15% | Key level, hard to break |
| **6894.76** | TECH | - | **SMA 50d** (Resistencia Dinámica) |
| **6880.0** | GAMMA_LEVEL | 10% | **Gamma Flip** (Zona de transición de volatilidad) |
| **6869.31** | TECH | - | **EMA 9d** (Tendencia Inmediata) |
| **6866.0** | LEVEL | 5% | **ZONA DE BATALLA** (Cierre 6863.51 vs 6866) |
| **6861.0** | MACRO | - | **Objetivo Primario** / Overnight futures zone |
| **6841-6846** | LEVEL | 8% | Key level |
| **6830.0** | LEVEL | 5% | Soporte menor |
| **6790.0** | REVERSAL | 15% | High likelihood of reversal / Mean Reversion |
| **6535.49** | TECH | - | SMA 200d (Soporte Estructural Largo Plazo) |

**ZONA DE BATALLA (6863.51)**: El cierre está a menos de 0.1% de la confluencia entre el nivel Quant **6866** y el objetivo Macro **6861**. La sesión se definirá por la resolución inmediata de este clúster.

### 5 Estructura de Opciones & Volatilidad
- **Régimen VIX**: Expansión/Ansiedad (19.92).
- **Entorno Gamma**: **NEGATIVO**. El precio (6863.51) está por debajo del nivel *Gamma Flip* (6880). Esto implica que los Market Makers deben vender en las caídas y comprar en las subidas, exacerbando la volatilidad direccional.
- **Alerta Bollinger**: El *Bollinger Width* está en un crítico **3.34%**. Esto es una "Squeeze" mayor. La explosión de volatilidad es inminente. Dado el sesgo bajista, la ruptura se espera hacia abajo.

### 6 Estrategias Operativas (Opciones)
*Obediencia a Narrativa: DOWN*

- **Estrategia**: **Long Put (Direccional)**. Aprovechando el Gamma Negativo y la inminente explosión de las Bandas de Bollinger.
- **Strike**: 6840 o 6850 (ITM/ATM).
- **Vencimiento**: Corto plazo (Semanal) debido a la naturaleza "front-loaded" de la debilidad.
- **STOP LOSS**: **Toque de 6880** (Gamma Flip). Si el precio toca 6880, el régimen de Gamma negativo se anula y la tesis bajista pierde fuerza inmediata.

### 7 Estrategias Operativas (Turbos)
*Obediencia a Narrativa: SHORT*

**🚀 Selección Inteligente (IA + Scanner Vontobel)**

| Parámetro | Valor | Notas Tácticas |
| :--- | :--- | :--- |
| **Producto** | **Turbo SHORT SPX** | ISIN: **DE000VJ1STJ4** |
| **KO (Barrera)** | **7030.00** | Riesgo de liquidación total. Situado sobre invalidación Macro (7000). |
| **Apalancamiento** | **36.1x** | Riesgo Alto. Ajustado para Squeeze de Bollinger. |

**Plan de Ejecución (Precios Teóricos)**

1.  **Entrada (Trigger)**:
    *   **SPX Nivel:** **6863.51** (Apertura/Mercado)
    *   **Turbo Precio:** **~1.66€**

2.  **Salida (Take Profit)**:
    *   **SPX Nivel:** **6861.00** (Objetivo Primario IA)
    *   **Turbo Precio:** **~1.69€**
    *   *Nota: Dado que el objetivo está muy cerca (scalping puro), si rompe 6861 con fuerza, gestionar trail stop, pero el plan estricto manda salida en 6861.*

3.  **Stop Loss (Emergencia)**:
    *   **SPX Nivel:** **7000.00** (Invalidación Macro)
    *   **Turbo Precio:** **~0.30€**

**⏱️ Gestión Temporal (Time-Stop Madrid)**
*   **Momento de Entrada**: **INMEDIATA (16:48 CET)**.
*   **Duración Estimada**: **~0h 15m** (Calculado por ATR/VIX/RSI/BB).
*   **Hora Límite**: **17:03 CET**.
*   **Instrucción**: Si a las 17:03 no se alcanza el objetivo (**1.69€**), CERRAR LA POSICIÓN a mercado.

### 8 Plan de Trading (Execution)
- **Tesis**: La debilidad estructural (bajo EMA 9d y SMA 50d) combinada con Gamma Negativo forzará una prueba inmediata de los mínimos.
- **Trigger**: Apertura por debajo de 6866.
- **Objetivo**: El objetivo obligatorio es **6861**.
- **Contingencia**: Si el precio recupera **6869** (EMA 9d), abortar misión bajista intradía. Si rompe **6861** con volumen, el siguiente nivel magnético de Quant es **6841**.

### 9 Gestión de Riesgo
- **Tamaño**: **REDUCIDO**. El VIX en 19.92 exige cautela. Usar 50% del tamaño habitual.
- **Convicción**: **MEDIA-ALTA**. Aunque el sesgo es "medium conviction", la confluencia técnica de la *Bollinger Squeeze* con el régimen de Gamma Negativo y la tendencia bajista (bajo EMA 9d) eleva la probabilidad de un movimiento explosivo.

### 10 Conclusión Final de TearAgent
La oportunidad con mejor asimetría es explotar el **Gamma Negativo bajo 6880** ante la inminente expansión de volatilidad señalada por el **Bollinger Squeeze (3.34%)**. El mercado está preparado para un movimiento brusco; la posición técnica sugiere que será a la baja hacia el objetivo de **6861** y potencialmente la zona de **6841**. Proteger estrictamente por encima de **6880**.
