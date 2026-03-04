package com.tearsdeepmind.service.market;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tearsdeepmind.domain.model.TurboProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VontobelScannerService {
    private static final Logger logger = LoggerFactory.getLogger(VontobelScannerService.class);
    private static final String SCRIPT_PATH = "/app/scripts/spx_scanner.py";
    
    private final ObjectMapper objectMapper;

    public VontobelScannerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<TurboProduct> scan(String underlyingSymbol, String direction) {
        if (!isSupported(underlyingSymbol)) {
            logger.error("Underlying {} not supported by Python scanner yet (S&P 500 only).", underlyingSymbol);
            return List.of();
        }

        String scanType = "LONG".equalsIgnoreCase(direction) ? "LONG" : "SHORT";
        logger.info("Invoking Python Scanner for {} {}...", underlyingSymbol, scanType);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "python3", 
                SCRIPT_PATH, 
                "--type", scanType, 
                "--limit", "15", 
                "--format", "json"
            );
            
            Process process = pb.start();
            
            // Capture stderr for logging (Background thread to avoid blocking)
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info("[PythonScanner] {}", line);
                    }
                } catch (Exception e) {
                    logger.warn("Error reading Python scanner stderr: {}", e.getMessage());
                }
            }).start();

            // Capture stdout for JSON data
            String jsonOutput;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                jsonOutput = reader.lines().collect(Collectors.joining("\n"));
            }

            // Wait for process completion (60s timeout)
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                logger.error("Python scanner timed out after 60 seconds. Destroying process.");
                process.destroyForcibly();
                return List.of();
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("Python scanner failed with exit code {}.", exitCode);
                return List.of();
            }

            if (jsonOutput == null || jsonOutput.isBlank() || jsonOutput.equals("[]")) {
                logger.warn("Python scanner returned no products.");
                return List.of();
            }

            // Map JSON to List<TurboProduct>
            List<PythonTurboDto> pythonResults = objectMapper.readValue(jsonOutput, new TypeReference<List<PythonTurboDto>>() {});
            
            List<TurboProduct> products = pythonResults.stream()
                .map(dto -> new TurboProduct(
                    dto.isin, 
                    dto.direction.toUpperCase(), 
                    dto.strike, 
                    dto.strike, // Using strike as KO for compatibility if KO not explicit
                    dto.leverage, 
                    java.math.BigDecimal.ZERO, // Bid/Ask not provided by basic scanner yet
                    java.math.BigDecimal.ZERO, 
                    new java.math.BigDecimal("0.01") // Default ratio for SPX
                ))
                .collect(Collectors.toList());

            logger.info("Successfully imported {} products from Python scanner.", products.size());
            return products;

        } catch (Exception e) {
            logger.error("Execution error in VontobelScannerService: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private boolean isSupported(String symbol) {
        return symbol != null && (symbol.toUpperCase().contains("SPX") || symbol.toUpperCase().contains("GSPC"));
    }

    // Internal DTO for Python JSON Mapping
    private static class PythonTurboDto {
        public String isin;
        public java.math.BigDecimal leverage;
        public java.math.BigDecimal strike;
        public String direction;
        public String underlying;
    }
}
