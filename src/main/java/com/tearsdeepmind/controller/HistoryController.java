package com.tearsdeepmind.controller;

import com.tearsdeepmind.entity.DailyAnalysisEntity;
import com.tearsdeepmind.entity.QuantMemoryEntity;
import com.tearsdeepmind.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@Tag(name = "History & Memory", description = "Endpoints for persisting and retrieving historical SPX analysis (Daily & Quant).")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    // Daily Analysis Endpoints
    @Operation(summary = "Get all Daily Analyses", description = "Retrieves the complete history of daily market context.")
    @GetMapping("/daily-analysis")
    public List<DailyAnalysisEntity> getAllDailyAnalysis() {
        return historyService.getAllDailyAnalysis();
    }

    @Operation(summary = "Get Daily Analysis by Date", description = "Retrieves specific daily analysis by YYYYMMDD date.")
    @GetMapping("/daily-analysis/{date}")
    public ResponseEntity<DailyAnalysisEntity> getDailyAnalysis(@PathVariable String date) {
        return historyService.getDailyAnalysis(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Save Daily Analysis", description = "Creates or updates the daily analysis JSON for a specific date.")
    @PostMapping("/daily-analysis/{date}")
    public DailyAnalysisEntity saveDailyAnalysis(@PathVariable String date, @RequestBody Map<String, Object> data) {
        return historyService.saveDailyAnalysis(date, data);
    }

    @Operation(summary = "Delete Daily Analysis", description = "Removes a daily analysis record.")
    @DeleteMapping("/daily-analysis/{date}")
    public ResponseEntity<Void> deleteDailyAnalysis(@PathVariable String date) {
        historyService.deleteDailyAnalysis(date);
        return ResponseEntity.noContent().build();
    }

    // Quant Memory Endpoints
    @Operation(summary = "Get all Quant Memories", description = "Retrieves full history of quantitative signals.")
    @GetMapping("/quant-memory")
    public List<QuantMemoryEntity> getAllQuantMemory() {
        return historyService.getAllQuantMemory();
    }

    @Operation(summary = "Get Quant Memory by Date", description = "Retrieves specific quant signals by YYYYMMDD date.")
    @GetMapping("/quant-memory/{date}")
    public ResponseEntity<QuantMemoryEntity> getQuantMemory(@PathVariable String date) {
        return historyService.getQuantMemory(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Save Quant Memory", description = "Creates or updates the quant memory JSON for a specific date.")
    @PostMapping("/quant-memory/{date}")
    public QuantMemoryEntity saveQuantMemory(@PathVariable String date, @RequestBody Map<String, Object> data) {
        return historyService.saveQuantMemory(date, data);
    }

    @Operation(summary = "Delete Quant Memory", description = "Removes a quant memory record.")
    @DeleteMapping("/quant-memory/{date}")
    public ResponseEntity<Void> deleteQuantMemory(@PathVariable String date) {
        historyService.deleteQuantMemory(date);
        return ResponseEntity.noContent().build();
    }
}
