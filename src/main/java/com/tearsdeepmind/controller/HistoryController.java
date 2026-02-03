package com.tearsdeepmind.controller;

import com.tearsdeepmind.domain.model.MarketMemoryRecord;
import com.tearsdeepmind.domain.model.QuantMemoryRecord;
import com.tearsdeepmind.entity.DailyAnalysisEntity;
import com.tearsdeepmind.entity.QuantMemoryEntity;
import com.tearsdeepmind.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/history")
@Tag(name = "History & Memory", description = "Endpoints for persisting and retrieving historical SPX analysis (Daily & Quant).")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @Operation(summary = "Get all Daily Analyses")
    @GetMapping("/daily-analysis")
    public List<DailyAnalysisEntity> getAllDailyAnalysis() {
        return historyService.getAllDailyAnalysis();
    }

    @Operation(summary = "Get Daily Analysis by Date")
    @GetMapping("/daily-analysis/{date}")
    public ResponseEntity<DailyAnalysisEntity> getDailyAnalysis(@PathVariable String date) {
        return historyService.getDailyAnalysis(LocalDate.parse(date))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Save Daily Analysis")
    @PostMapping("/daily-analysis/{date}")
    public DailyAnalysisEntity saveDailyAnalysis(@PathVariable String date, @RequestBody MarketMemoryRecord data) {
        return historyService.saveDailyAnalysis(LocalDate.parse(date), data);
    }

    @Operation(summary = "Delete Daily Analysis")
    @DeleteMapping("/daily-analysis/{date}")
    public ResponseEntity<Void> deleteDailyAnalysis(@PathVariable String date) {
        historyService.deleteDailyAnalysis(LocalDate.parse(date));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all Quant Memories")
    @GetMapping("/quant-memory")
    public List<QuantMemoryEntity> getAllQuantMemory() {
        return historyService.getAllQuantMemory();
    }

    @Operation(summary = "Get Quant Memory by Date")
    @GetMapping("/quant-memory/{date}")
    public ResponseEntity<QuantMemoryEntity> getQuantMemory(@PathVariable String date) {
        return historyService.getQuantMemory(LocalDate.parse(date))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Save Quant Memory")
    @PostMapping("/quant-memory/{date}")
    public QuantMemoryEntity saveQuantMemory(@PathVariable String date, @RequestBody QuantMemoryRecord data) {
        return historyService.saveQuantMemory(LocalDate.parse(date), data);
    }

    @Operation(summary = "Delete Quant Memory")
    @DeleteMapping("/quant-memory/{date}")
    public ResponseEntity<Void> deleteQuantMemory(@PathVariable String date) {
        historyService.deleteQuantMemory(LocalDate.parse(date));
        return ResponseEntity.noContent().build();
    }
}