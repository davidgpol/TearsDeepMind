# TurboCrawler 🚀

**TurboCrawler** es una herramienta de automatización y web scraping diseñada para extraer datos financieros en tiempo real de **Turbo Warrants** directamente desde las fuentes oficiales de los emisores.

A diferencia de los scrapers tradicionales que analizan el DOM visual (propenso a errores), TurboCrawler utiliza una técnica de **extracción de estado de hidratación** para obtener datos precisos directamente del backend de las aplicaciones modernas (Next.js).

## ✨ Características

- **Vontobel Support:** Extracción robusta mediante el análisis del blob `__NEXT_DATA__`.
- **Detección Automática:** Resolución de URLs dinámicas a través de servicios de redirección CMS.
- **Datos en Tiempo Real:** Obtiene precios (Bid/Ask), Apalancamiento, Strike, Barrera (KO) y Ratios.
- **Cálculo Inteligente:** Capacidad de calcular el apalancamiento efectivo si no está disponible explícitamente.
- **Salida Normalizada:** Formato JSON limpio listo para ser integrado en otras herramientas de trading.

## 🛠️ Cómo Funciona

El crawler sigue este flujo:
1. Recibe un código **ISIN**.
2. Identifica el emisor probable.
3. Resuelve la URL del producto utilizando el referer adecuado para evitar bloqueos.
4. Extrae el JSON interno de la aplicación web.
5. Normaliza y retorna los datos.

## 🚀 Instalación y Uso

### Requisitos
- Python 3.x
- Bibliotecas: `requests`, `beautifulsoup4`

### Ejecución
```bash
python fetch_turbo_data.py <ISIN>
```

Ejemplo:
```bash
python fetch_turbo_data.py DE000VH5LYF0
```

## 🚧 Hoja de Ruta
- [x] Soporte base para Vontobel.
- [ ] Implementar soporte para Société Générale.
- [ ] Soporte para BNP Paribas.
- [ ] Exportación a CSV/Base de Datos.

---
*Este proyecto es una Prueba de Concepto (PoC) con fines educativos y de análisis financiero.*
