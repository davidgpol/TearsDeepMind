package com.tearsdeepmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class MigrationService {

    private static final Logger logger = LoggerFactory.getLogger(MigrationService.class);
    private final HistoryService historyService;
    private final Path rootDir = Paths.get("TearsMind"); // Relative to project root
    private final Pattern DATE_PATTERN = Pattern.compile("^\\d{8}$");

    public MigrationService(HistoryService historyService) {
        this.historyService = historyService;
    }

    public void migrateAll() {
        logger.info("Starting historical migration from {}", rootDir.toAbsolutePath());
        
        if (!Files.exists(rootDir)) {
            logger.error("Root directory {} does not exist!", rootDir);
            return;
        }

        try (Stream<Path> paths = Files.list(rootDir)) {
            paths.filter(Files::isDirectory)
                 .filter(path -> DATE_PATTERN.matcher(path.getFileName().toString()).matches())
                 .forEach(this::processDateFolder);
        } catch (IOException e) {
            logger.error("Error walking directory structure", e);
        }
        
        logger.info("Migration completed.");
    }

    private void processDateFolder(Path datePath) {
        String date = datePath.getFileName().toString();
        logger.info("Processing date: {}", date);

        processSection(datePath, "DailyAnalysis", date, true);
        processSection(datePath, "QuantUpdates", date, false);
    }

    private void processSection(Path datePath, String sectionName, String date, boolean isDaily) {
        Path sectionPath = datePath.resolve(sectionName);
        if (!Files.exists(sectionPath)) return;

        try (Stream<Path> files = Files.list(sectionPath)) {
            files.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".txt"))
                 .findFirst() // Assuming one report per section per day usually
                 .ifPresent(file -> {
                     try {
                         String content = Files.readString(file);
                         saveToDb(date, file.getFileName().toString(), content, isDaily);
                     } catch (IOException e) {
                         logger.error("Failed to read file {}", file, e);
                     }
                 });
        } catch (IOException e) {
            logger.error("Error listing files in {}", sectionPath, e);
        }
    }

    private void saveToDb(String date, String fileName, String content, boolean isDaily) {
        Map<String, Object> data = new HashMap<>();
        data.put("source_file", fileName);
        data.put("raw_content", content);
        data.put("migrated_at", LocalDateTime.now().toString());
        data.put("status", "RAW_IMPORTED"); // Marker that this is raw text, not processed JSON schema

        if (isDaily) {
            // Check if exists to avoid overwrite if we want (or just overwrite)
            // For now, overwrite/update
//            historyService.saveDailyAnalysis(date, data);
//            logger.debug("Saved DailyAnalysis for {}", date);
//        } else {
//            historyService.saveQuantMemory(date, data);
            logger.debug("Saved QuantMemory for {}", date);
        }
    }
}
