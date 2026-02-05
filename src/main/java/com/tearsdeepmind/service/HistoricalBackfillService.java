package com.tearsdeepmind.service;

import com.tearsdeepmind.repository.RawDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@Service
public class HistoricalBackfillService {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalBackfillService.class);
    
    private final IngestionService ingestionService;
    private final PipelineService pipelineService;

    @Value("${crawler.output.dir:/app/TearsMind}")
    private String volumePath;

    public HistoricalBackfillService(IngestionService ingestionService, PipelineService pipelineService) {
        this.ingestionService = ingestionService;
        this.pipelineService = pipelineService;
    }

    public void runBackfill() {
        logger.info("Starting Historical Backfill from {}", volumePath);
        Path root = Paths.get(volumePath);
        
        try (Stream<Path> dates = Files.list(root)) {
            dates.filter(Files::isDirectory)
                 .filter(p -> p.getFileName().toString().matches("\\d{8}"))
                 .sorted()
                 .forEach(this::processDateFolder);
        } catch (IOException e) {
            logger.error("Failed to list volume directory", e);
        }
    }

    private void processDateFolder(Path dateFolder) {
        String folderName = dateFolder.getFileName().toString();
        LocalDate date = LocalDate.parse(folderName, DateTimeFormatter.ofPattern("yyyyMMdd"));
        logger.info("--- Processing Date: {} ---", date);

        try {
            // 1. Ingest Macro
            ingestType(dateFolder.resolve("DailyAnalysis"), date, "MACRO");
            
            // 2. Ingest Quant
            ingestType(dateFolder.resolve("QuantUpdates"), date, "QUANT");

            // 3. Process Intelligence & Report (Full Reprocess)
            logger.info("Triggering analysis reprocessing for {}", date);
            pipelineService.runPipeline(date);
        } catch (Exception e) {
            logger.error("CRITICAL BACKFILL ERROR for date: " + date + ". Skipping to next date.", e);
        }
    }

    private void ingestType(Path folder, LocalDate date, String type) {
        if (!Files.exists(folder)) return;
        try (Stream<Path> files = Files.list(folder)) {
            Path txtFile = files.filter(p -> p.toString().endsWith(".txt")).findFirst().orElse(null);
            if (txtFile != null) {
                String content = Files.readString(txtFile);
                ingestionService.saveRawDocument(date, type, content);
            }
        } catch (IOException e) {
            logger.error("Failed to ingest {} for {}", type, date);
        }
    }
}
