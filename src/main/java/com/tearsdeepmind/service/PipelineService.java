package com.tearsdeepmind.service;

import com.tearsdeepmind.domain.model.MarketMemoryRecord;
import com.tearsdeepmind.domain.model.QuantMemoryRecord;
import com.tearsdeepmind.entity.DailyAnalysisEntity;
import com.tearsdeepmind.entity.QuantMemoryEntity;
import com.tearsdeepmind.entity.RawDocumentEntity;
import com.tearsdeepmind.entity.ReportEntity;
import com.tearsdeepmind.repository.DailyAnalysisRepository;
import com.tearsdeepmind.repository.QuantMemoryRepository;
import com.tearsdeepmind.repository.ReportRepository;
import com.tearsdeepmind.repository.PredictionRepository;
import com.tearsdeepmind.repository.QuantSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PipelineService {

    private static final Logger logger = LoggerFactory.getLogger(PipelineService.class);

    private final CrawlerService crawlerService;
    private final TearsAgentService tearsAgentService;
    private final IngestionService ingestionService;
    private final DailyAnalysisRepository dailyAnalysisRepository;
    private final QuantMemoryRepository quantMemoryRepository;
    private final ReportRepository reportRepository;
    private final EmailService emailService;
    private final MarketDataService marketDataService;
    private final TechnicalIndicatorService technicalIndicatorService;
    private final AuditService auditService;
    private final com.tearsdeepmind.repository.market.TechnicalIndicatorRepository technicalIndicatorRepository;
    private final com.tearsdeepmind.repository.market.AuditLogRepository auditLogRepository;
    private final com.tearsdeepmind.repository.market.DailyCandleRepository dailyCandleRepository;
    private final PredictionRepository predictionRepository;
    private final QuantSnapshotRepository quantSnapshotRepository;
    private final com.tearsdeepmind.service.market.VontobelScannerService vontobelScannerService;

    public PipelineService(CrawlerService crawlerService, 
                           TearsAgentService tearsAgentService, 
                           IngestionService ingestionService,
                           DailyAnalysisRepository dailyAnalysisRepository, 
                           QuantMemoryRepository quantMemoryRepository, 
                           ReportRepository reportRepository,
                           EmailService emailService,
                           MarketDataService marketDataService,
                           TechnicalIndicatorService technicalIndicatorService,
                           AuditService auditService,
                           com.tearsdeepmind.repository.market.TechnicalIndicatorRepository technicalIndicatorRepository,
                           com.tearsdeepmind.repository.market.AuditLogRepository auditLogRepository,
                           com.tearsdeepmind.repository.market.DailyCandleRepository dailyCandleRepository,
                           PredictionRepository predictionRepository,
                           QuantSnapshotRepository quantSnapshotRepository,
                           com.tearsdeepmind.service.market.VontobelScannerService vontobelScannerService) {
        this.crawlerService = crawlerService;
        this.tearsAgentService = tearsAgentService;
        this.ingestionService = ingestionService;
        this.dailyAnalysisRepository = dailyAnalysisRepository;
        this.quantMemoryRepository = quantMemoryRepository;
        this.reportRepository = reportRepository;
        this.emailService = emailService;
        this.marketDataService = marketDataService;
        this.technicalIndicatorService = technicalIndicatorService;
        this.auditService = auditService;
        this.technicalIndicatorRepository = technicalIndicatorRepository;
        this.auditLogRepository = auditLogRepository;
        this.dailyCandleRepository = dailyCandleRepository;
        this.predictionRepository = predictionRepository;
        this.quantSnapshotRepository = quantSnapshotRepository;
        this.vontobelScannerService = vontobelScannerService;
    }

    private String getTechnicalContextAsString(LocalDate date) {
        StringBuilder sb = new StringBuilder();
        
        // Determine Market Status
        java.time.ZonedDateTime nowNY = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/New_York"));
        boolean isMarketActive = nowNY.getHour() >= 9 && (nowNY.getHour() < 16 || (nowNY.getHour() == 16 && nowNY.getMinute() == 0)) && nowNY.getDayOfWeek().getValue() <= 5;
        
        String statusLabel = isMarketActive && date.equals(nowNY.toLocalDate()) ? "[LIVE/INTRADAY]" : "[SESSION CLOSED]";
        String priceLabelTag = isMarketActive && date.equals(nowNY.toLocalDate()) ? "CURRENT PRICE" : "CLOSING PRICE";

        List.of("^GSPC", "^VIX", "^TNX").forEach(symbol -> {
            dailyCandleRepository.findBySymbolAndDate(symbol, date).ifPresent(c -> {
                sb.append(String.format("[%s - %s]\n", symbol, statusLabel));
                sb.append(String.format("   - %s: %.2f\n", priceLabelTag, c.getClose()));
                
                technicalIndicatorRepository.findBySymbolAndDate(symbol, date).ifPresent(ti -> {
                    if (ti.getEma9d() != null) {
                        String trend = c.getClose().compareTo(ti.getEma9d()) > 0 ? "ABOVE" : "BELOW";
                        sb.append(String.format("   - Trend (EMA 9d): %s (%.2f)\n", trend, ti.getEma9d()));
                    }
                    if (symbol.equals("^GSPC") && ti.getSma50d() != null) {
                        sb.append("   - SMA 50d: ").append(ti.getSma50d()).append("\n");
                    }
                    if (symbol.equals("^GSPC") && ti.getSma200d() != null) {
                        sb.append("   - SMA 200d: ").append(ti.getSma200d()).append("\n");
                    }
                });
            });
            sb.append("\n");
        });
        
        return sb.length() > 0 ? sb.toString() : "No technical data available.";
    }

    private String getAuditVerdictAsString(LocalDate date) {
        return auditLogRepository.findByDate(date)
                .map(log -> "Veredicto del Juez para ayer: " + log.getVerdictSummary())
                .orElse("No hay auditoría previa disponible.");
    }

    @Transactional
    public Map<String, Object> runPipeline(LocalDate date) {
        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        
        try {
            // STEP 0: Market Data Sync & Indicators for all key symbols
            List<String> symbols = List.of("^GSPC", "^VIX", "^TNX");
            logger.info("Step 0: Syncing market data and indicators for symbols {}...", symbols);
            
            for (String symbol : symbols) {
                marketDataService.syncDailyData(symbol, "1y");
                // For intraday, best effort only for the primary asset (SPX) for now to save bandwidth
                if (symbol.equals("^GSPC")) {
                    marketDataService.syncIntradayData(symbol, date);
                }
                
                // Calculate technicals for the target date minus 1 (to have context for the session)
                // and for the target date itself if it's already closed/available
                technicalIndicatorService.calculateAndSaveIndicators(symbol, date.minusDays(1));
                technicalIndicatorService.calculateAndSaveIndicators(symbol, date);
            }
            
            // Performance audit for the PREVIOUS day
            auditService.auditDay(date.minusDays(1));

            // OPTIMIZATION: Check if report already exists
            String markdownReport;
            MarketMemoryRecord marketData = null;
            QuantMemoryRecord quantData = null;

            Optional<ReportEntity> existingReport = reportRepository.findById(date);

            if (existingReport.isPresent()) {
                logger.info("Report for {} already exists. Skipping AI generation. Audit performed.", date);
                markdownReport = existingReport.get().getContent();
                result.put("report_generated", false);
                result.put("audit_performed", true);
                
                // Try to recover Market Data context for Email Subject (Bias)
                // We fetch the DailyAnalysisEntity and try to map it back if needed, 
                // but for now we proceed. If needed, we could inject ObjectMapper to deserialize.
                
            } else {
                // STEP 1: Ingestion (Crawler -> BD)
                ensureRawDocumentsPresent(date);

                // STEP 2: Intelligence (BD Ingestion -> BD Analysis)
                quantData = processQuant(date).orElse(null);
                marketData = processMacro(date).orElse(null);
                
                result.put("quant_present", quantData != null);
                result.put("macro_present", marketData != null);

                if (quantData == null && marketData == null) {
                    throw new RuntimeException("No data available to process for date: " + date);
                }

                // STEP 2.5: Turbo Strategy Selection
                String turboStrategyBlock = "### 7 Estrategias Operativas (Turbos)\n*No se pudo generar estrategia automática.*";
                
                if (marketData != null && marketData.structured_prediction() != null) {
                    try {
                        String direction = marketData.structured_prediction().direction();
                        logger.info("Turbo Strategy: Prediction Direction is {}", direction);
                        
                        // Get current spot price
                        Double currentSpot = dailyCandleRepository.findBySymbolAndDate("^GSPC", date)
                                .map(c -> c.getClose().doubleValue())
                                .orElse(null);
                        
                        logger.info("Turbo Strategy: Current Spot for {} is {}", date, currentSpot);
                                
                        if (currentSpot != null && ("UP".equalsIgnoreCase(direction) || "DOWN".equalsIgnoreCase(direction))) {
                            Double stopLossLevel = "UP".equalsIgnoreCase(direction) 
                                    ? marketData.structured_prediction().expected_range_bottom() 
                                    : marketData.structured_prediction().expected_range_top();
                            
                            Double targetLevel = marketData.structured_prediction().primary_target();
                            
                            if (stopLossLevel == null || stopLossLevel == 0.0) {
                                stopLossLevel = "UP".equalsIgnoreCase(direction) ? currentSpot * 0.99 : currentSpot * 1.01;
                            }
                            
                            logger.info("Turbo Strategy: Calculated Stop Loss: {}, Target: {}", stopLossLevel, targetLevel);
                            
                            // Safe Scan direction mapping
                            String scanDirection = "UP".equalsIgnoreCase(direction) ? "LONG" : "SHORT";
                            List<com.tearsdeepmind.domain.model.TurboProduct> products = vontobelScannerService.scan("^GSPC", scanDirection);
                            
                            logger.info("Turbo Strategy: Scanner returned {} products.", products.size());
                            
                            Double finalStop = stopLossLevel;
                            com.tearsdeepmind.domain.model.TurboProduct bestTurbo = products.stream()
                                .filter(p -> isSafeKO(p, scanDirection, finalStop))
                                .findFirst()
                                .orElse(null);
                                
                            if (bestTurbo != null) {
                                logger.info("Turbo Strategy: Selected Best Turbo: {}", bestTurbo.isin());
                                turboStrategyBlock = buildTurboStrategyBlock(bestTurbo, currentSpot, targetLevel, stopLossLevel);
                            } else {
                                logger.warn("Turbo Strategy: No suitable turbo found after filtering.");
                            }
                        } else {
                            logger.warn("Turbo Strategy: Missing spot price or invalid direction.");
                        }
                    } catch (Exception e) {
                        logger.error("Failed to generate turbo strategy", e);
                    }
                } else {
                    logger.warn("Turbo Strategy: Market Data or Structured Prediction missing.");
                }

                // STEP 3: Synthesis (BD Analysis -> BD Reports)
                String marketReality = getTechnicalContextAsString(date);
                String auditVerdict = getAuditVerdictAsString(date.minusDays(1));

                markdownReport = tearsAgentService.generateFinalReport(date, quantData, marketData, marketReality, auditVerdict, turboStrategyBlock);
                
                ReportEntity reportEntity = new ReportEntity(
                    date, 
                    markdownReport, 
                    quantData != null ? ingestionService.getRawDocument(date, "QUANT").map(RawDocumentEntity::getId).orElse(null) : null,
                    marketData != null ? ingestionService.getRawDocument(date, "MACRO").map(RawDocumentEntity::getId).orElse(null) : null
                );
                reportRepository.save(reportEntity);
                result.put("report_generated", true);

                // STEP 3.1: Persist Structured Intelligence
                if (quantData != null && quantData.extracted_levels() != null) {
                    quantData.extracted_levels().forEach(level -> {
                        if (level.numeric_values() != null && !level.numeric_values().isEmpty()) {
                            java.math.BigDecimal value = java.math.BigDecimal.valueOf(level.numeric_values().get(0));
                            com.tearsdeepmind.entity.QuantSnapshotEntity snapshot = new com.tearsdeepmind.entity.QuantSnapshotEntity(
                                date, 
                                level.type() != null ? level.type().toUpperCase() : "LEVEL",
                                value,
                                null
                            );
                            quantSnapshotRepository.save(snapshot);
                        }
                    });
                    logger.info("Saved structured snapshots linked to report {}", date);
                }

                if (marketData != null && marketData.structured_prediction() != null) {
                    java.util.Map<String, Object> payload = new java.util.HashMap<>();
                    payload.put("direction", marketData.structured_prediction().direction());
                    payload.put("volatility", marketData.structured_prediction().volatility());
                    payload.put("primary_target", marketData.structured_prediction().primary_target());
                    payload.put("expected_range_top", marketData.structured_prediction().expected_range_top());
                    payload.put("expected_range_bottom", marketData.structured_prediction().expected_range_bottom());

                    com.tearsdeepmind.entity.PredictionEntity prediction = new com.tearsdeepmind.entity.PredictionEntity(
                        date, 
                        date, 
                        payload
                    );
                    predictionRepository.save(prediction);
                    logger.info("Saved structured prediction with full range linked to report {}", date);
                }
            }

            // STEP 4: Distribution (Runs for both new and existing reports)
            try {
                emailService.sendReport(date, markdownReport, marketData);
                result.put("notification_sent", true);
            } catch (Exception e) {
                logger.error("Notification failed", e);
                result.put("notification_sent", false);
            }

            result.put("status", "SUCCESS");

        } catch (Exception e) {
            logger.error("Pipeline failed for date: " + date, e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }
        return result;
    }

    private void ensureRawDocumentsPresent(LocalDate date) {
        if (ingestionService.getRawDocument(date, "QUANT").isEmpty() || 
            ingestionService.getRawDocument(date, "MACRO").isEmpty()) {
            
            logger.info("Raw documents missing for {}. Triggering crawler...", date);
            String uuid = java.util.UUID.randomUUID().toString().substring(0,8);
            
            // Sync extraction for pipeline (Waiting for Completion)
            // PASS DATE TO CRAWLER TO FORCE CORRECT ASSIGNMENT ON FALLBACK
            java.util.concurrent.CompletableFuture<Void> macroTask = crawlerService.extract("DailyAnalysis", 1, uuid + "-m", date);
            java.util.concurrent.CompletableFuture<Void> quantTask = crawlerService.extract("QuantUpdates", 1, uuid + "-q", date);
            
            java.util.concurrent.CompletableFuture.allOf(macroTask, quantTask).join();
            logger.info("Crawler tasks finished for {}. Resuming pipeline.", date);
        }
    }

    private Optional<QuantMemoryRecord> processQuant(LocalDate date) {
        return ingestionService.getRawDocument(date, "QUANT").map(doc -> {
            QuantMemoryRecord record = tearsAgentService.extractQuantIntelligence(doc.getContent());
            quantMemoryRepository.save(new QuantMemoryEntity(date, record, doc.getId(), "v1", "gemini-2.0-flash"));
            return record;
        });
    }

    private Optional<MarketMemoryRecord> processMacro(LocalDate date) {
        return ingestionService.getRawDocument(date, "MACRO").map(doc -> {
            MarketMemoryRecord record = tearsAgentService.extractMacroIntelligence(doc.getContent());
            dailyAnalysisRepository.save(new DailyAnalysisEntity(date, record, doc.getId(), "v1", "gemini-2.0-flash"));
            return record;
        });
    }

    private boolean isSafeKO(com.tearsdeepmind.domain.model.TurboProduct p, String direction, Double stopLoss) {
        if (p.barrier() == null) return false;
        double ko = p.barrier().doubleValue();
        // Buffer safety: KO must be further than Stop Loss by at least 5 points
        return "LONG".equalsIgnoreCase(direction) ? ko < (stopLoss - 5) : ko > (stopLoss + 5);
    }

    private String buildTurboStrategyBlock(com.tearsdeepmind.domain.model.TurboProduct p, Double entrySpx, Double targetSpx, Double stopSpx) {
        double ratio = p.ratio() != null ? p.ratio().doubleValue() : 0.01;
        double strike = p.strike().doubleValue();
        boolean isLong = "LONG".equalsIgnoreCase(p.direction());

        // Pricing Engine
        double entryPrice = Math.max(0.01, isLong ? (entrySpx - strike) * ratio : (strike - entrySpx) * ratio);
        double targetPrice = Math.max(0.01, isLong ? (targetSpx - strike) * ratio : (strike - targetSpx) * ratio);
        double stopPrice = Math.max(0.01, isLong ? (stopSpx - strike) * ratio : (strike - stopSpx) * ratio);

        // Time Engine (Simplified V1 - Until ATR is ready)
        // Using static estimation for now as requested by user plan (Technical Engine is Phase 2)
        String duration = "~2h 15m"; 
        String exitTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/Madrid")).plusHours(2).plusMinutes(15).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        return String.format("""
### 7 Estrategias Operativas (Turbos)
*Obediencia a Narrativa: %s*

**🚀 Selección Inteligente (IA + Scanner Vontobel)**

| Parámetro | Valor | Notas Tácticas |
| :--- | :--- | :--- |
| **Producto** | **Turbo %s SPX** | ISIN: **%s** |
| **KO (Barrera)** | **%.2f** | Riesgo de liquidación total. |
| **Apalancamiento** | **%.1fx** | Riesgo Alto. |

**Plan de Ejecución (Precios Teóricos)**

1.  **Entrada (Trigger)**:
    *   **SPX Nivel:** **%.2f**
    *   **Turbo Precio:** **~%.2f€**

2.  **Salida (Take Profit)**:
    *   **SPX Nivel:** **%.2f**
    *   **Turbo Precio:** **~%.2f€**

3.  **Stop Loss (Emergencia)**:
    *   **SPX Nivel:** **%.2f**
    *   **Turbo Precio:** **~%.2f€**

**⏱️ Gestión Temporal (Time-Stop Madrid)**
*   **Duración Estimada**: **%s**.
*   **Hora Límite**: **%s CET**.
*   **Instrucción**: Si a las %s no se alcanza el objetivo (**%.2f€**), CERRAR LA POSICIÓN a mercado.
""", 
        p.direction(), p.direction(), p.isin(), p.barrier(), p.leverage(),
        entrySpx, entryPrice,
        targetSpx, targetPrice,
        stopSpx, stopPrice,
        duration, exitTime, exitTime, targetPrice);
    }
}
