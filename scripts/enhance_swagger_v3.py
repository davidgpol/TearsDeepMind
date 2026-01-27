import json
import os
import copy

# Configuración
BASE_PROJECT = "/home/ubuntu/.gemini/Dev/Projects/TearsDeepMind"
INPUT_PATH = os.path.join(BASE_PROJECT, "src/main/resources/collections/TearsDeepMind_Master_API.json")
OUTPUT_PATH = os.path.join(BASE_PROJECT, "src/main/resources/collections/TearsDeepMind_ClickReady.json")
SAMPLE_MARKET = "/home/ubuntu/.gemini/Dev/Volumes/TearsMind/20260123/market_memory_20260123.json"
SAMPLE_QUANT = "/home/ubuntu/.gemini/Dev/Volumes/TearsMind/20260123/quant_memory_20260123.json"

def load_json(path):
    if not os.path.exists(path): return None
    with open(path, 'r') as f: return json.load(f)

def main():
    spec = load_json(INPUT_PATH)
    if not spec:
        print(f"Error: No se encontró {INPUT_PATH}")
        return

    # 1. Definir Valores por Defecto (Click-to-Run)
    defaults = {
        "seccion": "DailyAnalysis",
        "dias": 5,
        "date": "2026-01-23",
        "jobId": "latest-job-uuid"
    }

    # 2. Cargar Ejemplos Reales
    market_sample = load_json(SAMPLE_MARKET)
    quant_sample = load_json(SAMPLE_QUANT)

    # 3. Procesar Rutas
    paths = spec.get("paths", {})
    new_paths = {}

    for path, methods in paths.items():
        # Categorización por Versión
        tag = "Unclassified"
        if "/api/v1/" in path: tag = "V1 (Standard/Sync)"
        elif "/api/v2/" in path: tag = "V2 (Industrial/Async)"
        elif "/api/history/" in path: tag = "Persistence (History API)"

        for method, operation in methods.items():
            # Asignar Tag Correcto
            operation["tags"] = [tag]
            
            # Inyectar Valores por Defecto en Parámetros
            if "parameters" in operation:
                for param in operation["parameters"]:
                    name = param.get("name")
                    if name in defaults:
                        # Para OpenAPI 3.0, el ejemplo va en schema o example
                        param["example"] = defaults[name]
                        # Postman a veces prefiere 'default' dentro de schema
                        if "schema" in param:
                            param["schema"]["default"] = defaults[name]
                            param["schema"]["example"] = defaults[name]

            # Inyectar Ejemplos Reales en Respuestas
            if "daily-analysis" in path and method == "get" and market_sample:
                 content = operation.get("responses", {}).get("200", {}).get("content", {}).get("*/*", {})
                 content["example"] = market_sample
            
            if "quant-memory" in path and method == "get" and quant_sample:
                 content = operation.get("responses", {}).get("200", {}).get("content", {}).get("*/*", {})
                 content["example"] = quant_sample

        new_paths[path] = methods

    # 4. Inyectar Endpoint Síncrono Perdido (Si falta)
    if "/api/v1/crawler/extract/{seccion}/{dias}" not in new_paths:
        new_paths["/api/v1/crawler/extract/{seccion}/{dias}"] = {
            "get": {
                "tags": ["V1 (Standard/Sync)"],
                "summary": "Sync Extract (Standard)",
                "description": "Trigger extraction blocking the client until completion. Good for single day checks.",
                "operationId": "extractSync",
                "parameters": [
                    {"name": "seccion", "in": "path", "required": True, "schema": {"type": "string", "default": "DailyAnalysis"}, "example": "DailyAnalysis"},
                    {"name": "dias", "in": "path", "required": True, "schema": {"type": "integer", "default": 5}, "example": 5}
                ],
                "responses": {"200": {"description": "OK"}}
            }
        }

    spec["paths"] = new_paths
    
    with open(OUTPUT_PATH, 'w') as f:
        json.dump(spec, f, indent=2)
    print(f"🎉 Colección Click-to-Run generada: {OUTPUT_PATH}")

if __name__ == "__main__":
    main()
