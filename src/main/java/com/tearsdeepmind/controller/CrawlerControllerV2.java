package com.tearsdeepmind.controller;

import com.tearsdeepmind.dto.CheckReportResponse;
import com.tearsdeepmind.model.ExtractionJob;
import com.tearsdeepmind.service.CrawlerService;
import com.tearsdeepmind.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v2/crawler")
@Tag(name = "Crawler V2", description = "New generation endpoints with enhanced determinism and real-time monitoring.")
public class CrawlerControllerV2 {

    private final CrawlerService crawlerService;
    private final MonitoringService monitoringService;

    @Autowired
    public CrawlerControllerV2(CrawlerService crawlerService, MonitoringService monitoringService) {
        this.crawlerService = crawlerService;
        this.monitoringService = monitoringService;
    }

    @Operation(summary = "Check Report Existence", description = "Deterministically checks if a report exists for a specific date on the remote platform.")
    @GetMapping("/check/{seccion}/{date}")
    public ResponseEntity<CheckReportResponse> checkReport(
            @PathVariable String seccion,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        CheckReportResponse response = crawlerService.checkReportExistence(seccion, date);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Start Industrial Extraction", description = "Starts a background extraction job using Virtual Threads and Session Sharing.")
    @PostMapping("/jobs/{seccion}/{dias}")
    public ResponseEntity<String> startJob(@PathVariable String seccion, @PathVariable int dias) {
        String jobId = crawlerService.startAsyncExtraction(seccion, dias);
        return ResponseEntity.accepted().body(jobId);
    }

    @Operation(summary = "Stream Real-time Events", description = "Subscribes to real-time events for a specific job via Server-Sent Events (SSE).")
    @GetMapping(value = "/stream/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable String jobId) {
        return monitoringService.subscribe(jobId);
    }
}
