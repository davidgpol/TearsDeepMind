package com.tearsdeepmind.controller;

import com.tearsdeepmind.dto.CheckReportResponse;
import com.tearsdeepmind.service.CrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v2/crawler")
@Tag(name = "Crawler V2", description = "New generation endpoints with enhanced determinism.")
public class CrawlerControllerV2 {

    private final CrawlerService crawlerService;

    @Autowired
    public CrawlerControllerV2(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @Operation(summary = "Check Report Existence", description = "Deterministically checks if a report exists for a specific date on the remote platform.")
    @GetMapping("/check/{seccion}/{date}")
    public ResponseEntity<CheckReportResponse> checkReport(
            @PathVariable String seccion,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        CheckReportResponse response = crawlerService.checkReportExistence(seccion, date);
        return ResponseEntity.ok(response);
    }
}
