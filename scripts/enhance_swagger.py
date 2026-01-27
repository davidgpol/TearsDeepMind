import json
import os

# Rutas
BASE_PROJECT = "/home/ubuntu/.gemini/Dev/Projects/TearsDeepMind"
COLLECTION_PATH = os.path.join(BASE_PROJECT, "src/main/resources/collections/TearsDeepMind_Master_API.json")
OUTPUT_PATH = os.path.join(BASE_PROJECT, "src/main/resources/collections/TearsDeepMind_Final_Collection.json")
SAMPLE_MARKET = "/home/ubuntu/.gemini/Dev/Volumes/TearsMind/20260123/market_memory_20260123.json"
SAMPLE_QUANT = "/home/ubuntu/.gemini/Dev/Volumes/TearsMind/20260123/quant_memory_20260123.json"

def load_json(path):
    with open(path, 'r') as f:
        return json.load(f)

def main():
    if not os.path.exists(COLLECTION_PATH):
        print("Error: Master API JSON not found.")
        return

    spec = load_json(COLLECTION_PATH)
    
    # 1. Inyectar Ejemplo Real: DailyAnalysisEntity
    if os.path.exists(SAMPLE_MARKET):
        market_data = load_json(SAMPLE_MARKET)
        # Buscar el esquema. SpringDoc a veces usa nombres generados.
        # Intentamos ubicar "DailyAnalysisEntity" o "MapStringObject"
        components = spec.get("components", {}).get("schemas", {})
        
        # Estrategia: Buscar cualquier esquema que se use en /api/history/daily-analysis
        # O inyectar directamente en el path si el esquema es genérico.
        
        # Vamos a inyectar el ejemplo en la definición del Path GET /api/history/daily-analysis/{date}
        paths = spec.get("paths", {})
        daily_get = paths.get("/api/history/daily-analysis/{date}", {}).get("get", {})
        if daily_get:
            responses = daily_get.get("responses", {}).get("200", {}).get("content", {}).get("*/*", {})
            if responses:
                responses["example"] = {"date": "2026-01-23", "data": market_data}
                print("✅ Inyectado ejemplo real en DailyAnalysis GET")

    # 2. Inyectar Ejemplo Real: QuantMemoryEntity
    if os.path.exists(SAMPLE_QUANT):
        quant_data = load_json(SAMPLE_QUANT)
        quant_get = paths.get("/api/history/quant-memory/{date}", {}).get("get", {})
        if quant_get:
            responses = quant_get.get("responses", {}).get("200", {}).get("content", {}).get("*/*", {})
            if responses:
                responses["example"] = {"date": "2026-01-23", "data": quant_data}
                print("✅ Inyectado ejemplo real en QuantMemory GET")

    # 3. Verificar/Inyectar Endpoints Legacy (Síncronos)
    # Check if /api/v1/crawler/extract/{seccion}/{dias} exists
    if "/api/v1/crawler/extract/{seccion}/{dias}" not in paths:
        print("⚠️ Endpoint Síncrono faltante. Inyectando...")
        paths["/api/v1/crawler/extract/{seccion}/{dias}"] = {
            "get": {
                "tags": ["Crawler Operations"],
                "summary": "Sync Extract (Legacy)",
                "description": "Triggers a legacy synchronous extraction task (blocks connection). Warning: May timeout on large requests.",
                "operationId": "extractSync",
                "parameters": [
                    {"name": "seccion", "in": "path", "required": True, "schema": {"type": "string"}},
                    {"name": "dias", "in": "path", "required": True, "schema": {"type": "integer"}}
                ],
                "responses": {
                    "200": {"description": "Extraction completed"}
                }
            }
        }
    
    # 4. Guardar
    with open(OUTPUT_PATH, 'w') as f:
        json.dump(spec, f, indent=2)
    print(f"🎉 Colección final generada en: {OUTPUT_PATH}")

if __name__ == "__main__":
    main()
