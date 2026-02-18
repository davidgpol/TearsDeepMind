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
                           QuantSnapshotRepository quantSnapshotRepository) {
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
    }

    private String getTechnicalContextAsString(LocalDate date) {
        StringBuilder sb = new StringBuilder();
        
        // Determine Market Status
        java.time.ZonedDateTime nowNY = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/New_York"));
        boolean isMarketActive = nowNY.getHour() >= 9 && (nowNY.getHour() < 16 || (nowNY.getHour() == 16 && nowNY.getMinute() == 0)) && nowNY.getDayOfWeek().getValue() <= 5;
        
        String statusLabel = isMarketActive && date.equals(nowNY.toLocalDate()) ? "[LIVE/INTRADAY]" : "[SESSION CLOSED]";
        String priceLabelTag = isMarketActive && date.equals(nowNY.toLocalDate()) ? "CURRENT PRICE" : "CLOSING PRICE";

        dailyCandleRepository.findBySymbolAndDate("^GSPC", date).ifPresent(c -> {
            sb.append(String.format("SPX %s:\n", statusLabel));
            sb.append(String.format("   - %s: %.2f\n", priceLabelTag, c.getClose()));
        });
        dailyCandleRepository.findBySymbolAndDate("^VIX", date).ifPresent(c -> {
            sb.append(String.format("VIX LAST: %.2f\n", c.getClose()));
        });
        
        technicalIndicatorRepository.findBySymbolAndDate("^GSPC", date).ifPresent(ti -> {
            sb.append("Technicals:\n");
            if (ti.getEma9d() != null) sb.append("- EMA 9d: ").append(ti.getEma9d()).append("\n");
            if (ti.getEma21d() != null) sb.append("- EMA 21d: ").append(ti.getEma21d()).append("\n");
            if (ti.getSma50d() != null) sb.append("- SMA 50d: ").append(ti.getSma50d()).append("\n");
            if (ti.getSma200d() != null) sb.append("- SMA 200d: ").append(ti.getSma200d()).append("\n");
            if (ti.getEma21w() != null) sb.append("- EMA 21w (Macro): ").append(ti.getEma21w()).append("\n");
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
            // STEP 0: Market Data Sync & Audit
            logger.info("Step 0: Syncing market data (1y range) and auditing performance for {}...", date);
            
            // FIX: Use 1y range to ensure coverage for historical backfills
            marketDataService.syncDailyData("^GSPC", "1y");
            marketDataService.syncDailyData("^VIX", "1y");
            
            // Sync intraday (Best effort for past dates)
            marketDataService.syncIntradayData("^GSPC", date);
            
            // Calculate technicals and perform audit for the PREVIOUS day
            technicalIndicatorService.calculateAndSaveIndicators("^GSPC", date.minusDays(1));
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

                // STEP 3: Synthesis (BD Analysis -> BD Reports)
                String marketReality = getTechnicalContextAsString(date);
                String auditVerdict = getAuditVerdictAsString(date.minusDays(1));

                markdownReport = tearsAgentService.generateFinalReport(date, quantData, marketData, marketReality, auditVerdict);
                
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

                    com.tearsdeepmind.entity.PredictionEntity prediction = new com.tearsdeepmind.entity.PredictionEntity(
                        date, 
                        date, 
                        payload
                    );
                    predictionRepository.save(prediction);
                    logger.info("Saved structured prediction linked to report {}", date);
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
}
