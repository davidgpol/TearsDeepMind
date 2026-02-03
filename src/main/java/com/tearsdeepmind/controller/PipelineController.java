package com.tearsdeepmind.controller;

import com.tearsdeepmind.service.HistoricalBackfillService;
import com.tearsdeepmind.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v2/pipeline")
@Tag(name = "Pipeline Orchestrator", description = "End-to-end automation triggers.")
public class PipelineController {

    private final PipelineService pipelineService;
    private final HistoricalBackfillService historicalBackfillService;

    public PipelineController(PipelineService pipelineService, HistoricalBackfillService historicalBackfillService) {
        System.out.println(">>> PIPELINE CONTROLLER LOADED <<<");
        this.pipelineService = pipelineService;
        this.historicalBackfillService = historicalBackfillService;
    }

    @Operation(summary = "Run Full Analysis Pipeline", description = "Triggers the full flow: Read Crawler Files -> Generate AI Analysis -> Persist JSONs.")
    @PostMapping("/run/{date}")
    public ResponseEntity<Map<String, Object>> runPipeline(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        return ResponseEntity.ok(pipelineService.runPipeline(date));
    }

    @Operation(summary = "Run Historical Backfill", description = "Scan entire volume and migrate/reprocess all missing data.")
    @PostMapping("/backfill")
    public ResponseEntity<String> runBackfill() {
        // Run in a separate thread to not block HTTP response
        CompletableFuture.runAsync(() -> historicalBackfillService.runBackfill());
        return ResponseEntity.accepted().body("Backfill task started successfully in background.");
    }
}
