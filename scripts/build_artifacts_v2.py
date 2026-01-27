import json
import os
import uuid

# --- Configuración ---
BASE_PROJECT_DIR = "/home/ubuntu/.gemini/Dev/Projects/TearsDeepMind"
COLLECTIONS_DIR = os.path.join(BASE_PROJECT_DIR, "src/main/resources/collections")
DOCS_DIR = os.path.join(BASE_PROJECT_DIR, "docs")
SAMPLES_DIR = "/home/ubuntu/.gemini/Dev/Volumes/TearsMind/20260123"

POSTMAN_OUTPUT_PATH = os.path.join(COLLECTIONS_DIR, "TearsDeepMind_CRUD_Ops.postman_collection.json")
DRAWIO_OUTPUT_PATH = os.path.join(DOCS_DIR, "TearsDeepMind_Arch.drawio")

SAMPLE_MARKET_PATH = os.path.join(SAMPLES_DIR, "market_memory_20260123.json")
SAMPLE_QUANT_PATH = os.path.join(SAMPLES_DIR, "quant_memory_20260123.json")

# --- Funciones Auxiliares ---
def load_json_sample(path):
    try:
        with open(path, 'r') as f:
            return json.dumps(json.load(f), indent=4)
    except FileNotFoundError:
        print(f"⚠️  Advertencia: No se encontró el archivo de ejemplo en {path}. El body del POST estará vacío.")
        return "{}"

# --- Lógica Principal ---
def build_postman_collection():
    market_body_sample = load_json_sample(SAMPLE_MARKET_PATH)
    quant_body_sample = load_json_sample(SAMPLE_QUANT_PATH)
    
    collection = {
        "info": {
            "_postman_id": str(uuid.uuid4()),
            "name": "TearsDeepMind CRUD API",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
        },
        "item": [
            {
                "name": "V2 (Industrial/Async)",
                "item": [
                    {"name": "Start DailyAnalysis Job", "request": {"method": "POST", "url": "{{baseUrl}}/api/v2/crawler/jobs/DailyAnalysis/5"}},
                    {"name": "Start QuantUpdates Job", "request": {"method": "POST", "url": "{{baseUrl}}/api/v2/crawler/jobs/QuantUpdates/5"}},
                    {"name": "Stream Job Events", "request": {"method": "GET", "url": "{{baseUrl}}/api/v2/crawler/stream/{{jobId}}"}}
                ]
            },
            {
                "name": "V1 (Standard/Sync)",
                "item": [
                    {"name": "Check New Reports", "request": {"method": "GET", "url": "{{baseUrl}}/api/v1/crawler/check/DailyAnalysis"}}
                ]
            },
            {
                "name": "Persistence (History API)",
                "item": [
                    {
                        "name": "Daily Analysis",
                        "item": [
                            {"name": "GET Record (23/01)", "request": {"method": "GET", "url": "{{baseUrl}}/api/history/daily-analysis/2026-01-23"}},
                            {
                                "name": "CREATE/UPDATE Record (23/01)",
                                "request": {
                                    "method": "POST",
                                    "header": [{"key": "Content-Type", "value": "application/json"}],
                                    "body": {"mode": "raw", "raw": market_body_sample, "options": {"raw": {"language": "json"}}},
                                    "url": "{{baseUrl}}/api/history/daily-analysis/2026-01-23"
                                }
                            },
                            {"name": "DELETE Record (23/01)", "request": {"method": "DELETE", "url": "{{baseUrl}}/api/history/daily-analysis/2026-01-23"}}
                        ]
                    },
                    {
                        "name": "Quant Memory",
                        "item": [
                            {"name": "GET Record (23/01)", "request": {"method": "GET", "url": "{{baseUrl}}/api/history/quant-memory/2026-01-23"}},
                            {
                                "name": "CREATE/UPDATE Record (23/01)",
                                "request": {
                                    "method": "POST",
                                    "header": [{"key": "Content-Type", "value": "application/json"}],
                                    "body": {"mode": "raw", "raw": quant_body_sample, "options": {"raw": {"language": "json"}}},
                                    "url": "{{baseUrl}}/api/history/quant-memory/2026-01-23"
                                }
                            },
                            {"name": "DELETE Record (23/01)", "request": {"method": "DELETE", "url": "{{baseUrl}}/api/history/quant-memory/2026-01-23"}}
                        ]
                    }
                ]
            }
        ],
        "variable": [
            {"key": "baseUrl", "value": "http://localhost:8080"},
            {"key": "jobId", "value": "latest-job-uuid"}
        ]
    }

    with open(POSTMAN_OUTPUT_PATH, 'w') as f:
        json.dump(collection, f, indent=4)
    print(f"✅ Colección Postman CRUD nativa generada en: {POSTMAN_OUTPUT_PATH}")

def build_drawio_diagram():
    xml_content = """<mxfile host="app.diagrams.net" agent="SeniorDeveloperAgent">
  <diagram name="TearsDeepMind Architecture">
    <mxGraphModel dx="1434" dy="754" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1169" pageHeight="827">
      <root>
        <mxCell id="0" />
        <mxCell id="1" parent="0" />
        <mxCell id="swim-client" value="Client Layer" style="swimlane;startSize=20;fontStyle=1" vertex="1" parent="1"><mxGeometry x="40" y="40" width="1040" height="120" as="geometry" /></mxCell>
        <mxCell id="node-postman" value="Postman / CLI" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;" vertex="1" parent="swim-client"><mxGeometry x="450" y="50" width="120" height="40" as="geometry" /></mxCell>
        <mxCell id="swim-api" value="API Layer (Tomcat)" style="swimlane;startSize=20;fontStyle=1" vertex="1" parent="1"><mxGeometry x="40" y="160" width="1040" height="200" as="geometry" /></mxCell>
        <mxCell id="node-cc1" value="CrawlerController (V1)" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#f8cecc;strokeColor=#b85450;" vertex="1" parent="swim-api"><mxGeometry x="150" y="70" width="140" height="60" as="geometry" /></mxCell>
        <mxCell id="node-cc2" value="CrawlerControllerV2" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;" vertex="1" parent="swim-api"><mxGeometry x="440" y="70" width="140" height="60" as="geometry" /></mxCell>
        <mxCell id="node-hc" value="HistoryController" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#e1d5e7;strokeColor=#9673a6;" vertex="1" parent="swim-api"><mxGeometry x="730" y="70" width="140" height="60" as="geometry" /></mxCell>
        <mxCell id="swim-service" value="Service Layer" style="swimlane;startSize=20;fontStyle=1" vertex="1" parent="1"><mxGeometry x="40" y="360" width="1040" height="200" as="geometry" /></mxCell>
        <mxCell id="node-cs" value="CrawlerService" style="rounded=0;whiteSpace=wrap;html=1;" vertex="1" parent="swim-service"><mxGeometry x="440" y="50" width="140" height="60" as="geometry" /></mxCell>
        <mxCell id="node-vt" value="Virtual Thread Pool" style="shape=cylinder;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=15;fillColor=#fff2cc;strokeColor=#d6b656;" vertex="1" parent="swim-service"><mxGeometry x="620" y="50" width="100" height="80" as="geometry" /></mxCell>
        <mxCell id="swim-infra" value="Infrastructure / Persistence" style="swimlane;startSize=20;fontStyle=1" vertex="1" parent="1"><mxGeometry x="40" y="560" width="1040" height="200" as="geometry" /></mxCell>
        <mxCell id="node-selenium" value="Selenium Grid" style="shape=cube;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;darkOpacity=0.05;fillColor=#ffe6cc;strokeColor=#d79b00;" vertex="1" parent="swim-infra"><mxGeometry x="320" y="70" width="120" height="80" as="geometry" /></mxCell>
        <mxCell id="node-db" value="PostgreSQL (JSONB)" style="shape=cylinder;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=15;fillColor=#f5f5f5;strokeColor=#666666;" vertex="1" parent="swim-infra"><mxGeometry x="740" y="70" width="100" height="80" as="geometry" /></mxCell>
        <mxCell id="edge-1" value="HTTP Request" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;" edge="1" parent="1" source="node-postman" target="node-cc1"><mxGeometry relative="1" as="geometry" /></mxCell>
        <mxCell id="edge-2" value="" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=classic;endFill=1;" edge="1" parent="1" source="node-postman" target="node-cc2"><mxGeometry relative="1" as="geometry" /></mxCell>
        <mxCell id="edge-3" value="" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=classic;endFill=1;" edge="1" parent="1" source="node-postman" target="node-hc"><mxGeometry relative="1" as="geometry" /></mxCell>
        <mxCell id="edge-4" value="Blocking Call" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#B85450;strokeWidth=2;" edge="1" parent="1" source="node-cc1" target="node-cs"><mxGeometry relative="1" as="geometry" /></mxCell>
        <mxCell id="edge-5" value="Spawns Task" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#82B366;strokeWidth=2;dashed=1;" edge="1" parent="1" source="node-cc2" target="node-vt"><mxGeometry relative="1" as="geometry" /></mxCell>
        <mxCell id="edge-6" value="Executes Logic" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;dashed=1;" edge="1" parent="1" source="node-vt" target="node-cs"><mxGeometry relative="1" as="geometry" /></mxCell>
        <mxCell id="edge-7" value="CRUD" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;" edge="1" parent="1" source="node-hc" target="node-db"><mxGeometry relative="1" as="geometry" /></mxCell>
        <mxCell id="edge-8" value="Drives Browser" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;" edge="1" parent="1" source="node-cs" target="node-selenium"><mxGeometry relative="1" as="geometry" /></mxCell>
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>"""
    with open(DRAWIO_OUTPUT_PATH, 'w') as f:
        f.write(xml_content)
    print(f"✅ Diagrama Draw.io nativo generado en: {DRAWIO_OUTPUT_PATH}")

if __name__ == "__main__":
    os.makedirs(COLLECTIONS_DIR, exist_ok=True)
    os.makedirs(DOCS_DIR, exist_ok=True)
    build_postman_collection()
    build_drawio_diagram()
