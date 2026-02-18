package com.tearsdeepmind.controller;

import com.tearsdeepmind.entity.market.AuditLogEntity;
import com.tearsdeepmind.entity.market.TechnicalIndicatorEntity;
import com.tearsdeepmind.repository.market.AuditLogRepository;
import com.tearsdeepmind.repository.market.TechnicalIndicatorRepository;
import com.tearsdeepmind.service.AuditService;
import com.tearsdeepmind.service.MarketDataService;
import com.tearsdeepmind.service.TechnicalIndicatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/market-data")
@Tag(name = "Market Data & Audit", description = "Endpoints for market reality syncing and performance auditing.")
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final TechnicalIndicatorService technicalIndicatorService;
    private final AuditService auditService;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final AuditLogRepository auditLogRepository;

    public MarketDataController(MarketDataService marketDataService,
                                TechnicalIndicatorService technicalIndicatorService,
                                AuditService auditService,
                                TechnicalIndicatorRepository technicalIndicatorRepository,
                                AuditLogRepository auditLogRepository) {
        this.marketDataService = marketDataService;
        this.technicalIndicatorService = technicalIndicatorService;
        this.auditService = auditService;
        this.technicalIndicatorRepository = technicalIndicatorRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @PostMapping("/sync")
    @Operation(summary = "Sync Market Data", description = "Downloads OHLCV data from Yahoo Finance for SPX and VIX.")
    public ResponseEntity<String> sync(@RequestParam(defaultValue = "5d") String range) {
        marketDataService.syncDailyData("^GSPC", range);
        marketDataService.syncDailyData("^VIX", range);
        marketDataService.syncIntradayData("^GSPC", LocalDate.now());
        return ResponseEntity.ok("Sync started for range: " + range);
    }

    @PostMapping("/backfill")
    @Operation(summary = "Historical Backfill", description = "Downloads extended historical data (e.g., 2y, 5y) with robust retry logic.")
    public ResponseEntity<String> backfill(@RequestParam String symbol, @RequestParam(defaultValue = "1y") String range) {
        marketDataService.syncHistoryWithRetry(symbol, range);
        return ResponseEntity.ok("Backfill triggered for " + symbol + " with range: " + range);
    }

    @PostMapping("/audit/{date}")
    @Operation(summary = "Manual Audit", description = "Runs the 'Judge' algorithm for a specific date.")
    public ResponseEntity<String> audit(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        technicalIndicatorService.calculateAndSaveIndicators("^GSPC", date);
        auditService.auditDay(date);
        return ResponseEntity.ok("Audit completed for date: " + date);
    }

    @GetMapping("/indicators/latest")
    @Operation(summary = "Get Latest Indicators", description = "Retrieves the computed technical indicators for the latest date.")
    public ResponseEntity<TechnicalIndicatorEntity> getLatestIndicators() {
        return technicalIndicatorRepository.findBySymbolAndDate("^GSPC", LocalDate.now().minusDays(1))
                .or(() -> technicalIndicatorRepository.findBySymbolAndDate("^GSPC", LocalDate.now()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/audit/{date}")
    @Operation(summary = "Get Audit Log", description = "Retrieves the performance audit for a specific date.")
    public ResponseEntity<AuditLogEntity> getAudit(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return auditLogRepository.findByDate(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
