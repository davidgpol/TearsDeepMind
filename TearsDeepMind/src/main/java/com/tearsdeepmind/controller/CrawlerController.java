package com.tearsdeepmind.controller;

import com.tearsdeepmind.service.CrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/crawler")
@Tag(name = "Crawler Operations", description = "Endpoints for triggering and monitoring forum crawling tasks.")
public class CrawlerController {

    private static final Logger logger = LogManager.getLogger(CrawlerController.class);

    private final CrawlerService crawlerService;

    @Autowired
    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @Operation(summary = "Extract threads from a specific section", description = "Triggers an asynchronous crawling task to extract forum threads from the specified section for the given number of days.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Extraction task started successfully."),
            @ApiResponse(responseCode = "500", description = "Internal server error starting the task.")
    })
    @GetMapping("/extract/{seccion}/{dias}")
    public CompletableFuture<Void> extract(
            @Parameter(description = "The section name to crawl (e.g., DailyAnalysis, QuantUpdates)", required = true, example = "DailyAnalysis") @PathVariable String seccion,
            @Parameter(description = "Number of days to look back for threads", required = true, example = "1") @PathVariable int dias) {
        String uuid = UUID.randomUUID().toString();
        logger.info("[{}] Received request to extract {} threads from section {}", uuid, dias, seccion);
        CompletableFuture<Void> future = crawlerService.extract(seccion, dias, uuid);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("[{}] Error during extraction.", uuid, ex);
            }
            logger.info("[{}] Finished extracting threads from section {}", uuid, seccion);
        });
        return future;
    }

    @Operation(summary = "Check for new content", description = "Scans the specified section feed and returns a list of thread titles that have not been downloaded to the local filesystem for the current day.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check completed successfully. Returns list of new thread titles."),
            @ApiResponse(responseCode = "500", description = "Internal server error during check.")
    })
    @GetMapping("/check/{seccion}")
    public java.util.List<String> check(
            @Parameter(description = "The section to check (e.g., DailyAnalysis, QuantUpdates)", required = true, example = "DailyAnalysis") @PathVariable String seccion) {
        String uuid = UUID.randomUUID().toString();
        logger.info("[{}] Received request to check for new threads in section {}", uuid, seccion);
        java.util.List<String> newThreads = crawlerService.checkForNewThreads(seccion);
        logger.info("[{}] Finished checking for new threads in section {}. Found: {}", uuid, seccion, newThreads.size());
        return newThreads;
    }
}