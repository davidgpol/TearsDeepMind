package com.tearsdeepmind.controller;

import com.tearsdeepmind.service.CrawlerService;
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
public class CrawlerController {

    private static final Logger logger = LogManager.getLogger(CrawlerController.class);

    private final CrawlerService crawlerService;

    @Autowired
    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @GetMapping("/extract/{seccion}/{dias}")
    public CompletableFuture<Void> extract(@PathVariable String seccion, @PathVariable int dias) {
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

    @GetMapping("/check/{seccion}")
    public boolean check(@PathVariable String seccion) {
        String uuid = UUID.randomUUID().toString();
        logger.info("[{}] Received request to check for new threads in section {}", uuid, seccion);
        boolean result = crawlerService.check(seccion);
        logger.info("[{}] Finished checking for new threads in section {}. Result: {}", uuid, seccion, result);
        return result;
    }
}
