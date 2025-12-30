package com.tearsdeepmind.controller;

import com.tearsdeepmind.model.ExtractionJob;
import com.tearsdeepmind.service.CrawlerService;
import com.tearsdeepmind.service.JobStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/crawler")
@Tag(name = "Crawler Operations", description = "Endpoints for triggering and monitoring forum crawling tasks.")
public class CrawlerController {

    private static final Logger logger = LogManager.getLogger(CrawlerController.class);

    private final CrawlerService crawlerService;
    private final JobStore jobStore;

    @Autowired
    public CrawlerController(CrawlerService crawlerService, JobStore jobStore) {
        this.crawlerService = crawlerService;
        this.jobStore = jobStore;
    }

    @Operation(summary = "Sync Extract", description = "Triggers a legacy synchronous extraction task (blocks connection).")
    @GetMapping("/extract/{seccion}/{dias}")
    public CompletableFuture<Void> extract(@PathVariable String seccion, @PathVariable int dias) {
        String uuid = UUID.randomUUID().toString();
        logger.info("[{}] Sync request for section {}", uuid, seccion);
        return crawlerService.extract(seccion, dias, uuid);
    }

    @Operation(summary = "Check for new content", description = "Scans feed and returns list of non-downloaded thread titles.")
    @GetMapping("/check/{seccion}")
    public List<String> check(@PathVariable String seccion) {
        return crawlerService.checkForNewThreads(seccion);
    }

    @Operation(summary = "Start Async Extraction (Industrial Mode)", description = "Starts a background extraction job with parallel processing, retries, and checkpointing.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Job accepted and started in background.")
    })
    @PostMapping("/async/extract/{seccion}/{dias}")
    public ResponseEntity<String> startAsync(@PathVariable String seccion, @PathVariable int dias) {
        String jobId = crawlerService.startAsyncExtraction(seccion, dias);
        logger.info("Started async job: {}", jobId);
        return ResponseEntity.accepted().body(jobId);
    }

    @Operation(summary = "Get Job Status", description = "Returns the current progress and status of an asynchronous job.")
    @GetMapping("/async/status/{jobId}")
    public ResponseEntity<ExtractionJob> getStatus(@PathVariable String jobId) {
        ExtractionJob job = jobStore.getJob(jobId);
        if (job == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(job);
    }
}