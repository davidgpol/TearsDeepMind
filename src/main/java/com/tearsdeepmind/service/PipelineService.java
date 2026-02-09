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
                           com.tearsdeepmind.repository.market.DailyCandleRepository dailyCandleRepository) {
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
    }

    private String getTechnicalContextAsString(LocalDate date) {
        StringBuilder sb = new StringBuilder();
        dailyCandleRepository.findBySymbolAndDate("^GSPC", date).ifPresent(c -> {
            sb.append(String.format("SPX Close: %.2f | ", c.getClose()));
        });
        dailyCandleRepository.findBySymbolAndDate("^VIX", date).ifPresent(c -> {
            sb.append(String.format("VIX: %.2f\n", c.getClose()));
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
            logger.info("Step 0: Syncing market data and auditing performance...");
            marketDataService.syncDailyData("^GSPC", "5d");
            marketDataService.syncDailyData("^VIX", "5d");
            marketDataService.syncIntradayData("^GSPC");
            
            technicalIndicatorService.calculateAndSaveIndicators("^GSPC", date.minusDays(1));
            auditService.auditDay(date.minusDays(1));

            // STEP 1: Ingestion (Crawler -> BD)
            // Note: The CrawlerService already calls ingestionService inside its extraction methods
            // We ensure we have the data by forcing a check/extract if needed
            ensureRawDocumentsPresent(date);

            // STEP 2: Intelligence (BD Ingestion -> BD Analysis)
            QuantMemoryRecord quantData = processQuant(date).orElse(null);
            MarketMemoryRecord marketData = processMacro(date).orElse(null);
            
            result.put("quant_present", quantData != null);
            result.put("macro_present", marketData != null);

            if (quantData == null && marketData == null) {
                throw new RuntimeException("No data available to process for date: " + date);
            }

            // STEP 3: Synthesis (BD Analysis -> BD Reports)
            String marketReality = getTechnicalContextAsString(date);
            String auditVerdict = getAuditVerdictAsString(date.minusDays(1));

            String markdownReport = tearsAgentService.generateFinalReport(date, quantData, marketData, marketReality, auditVerdict);
            
            ReportEntity reportEntity = new ReportEntity(
                date, 
                markdownReport, 
                quantData != null ? ingestionService.getRawDocument(date, "QUANT").map(RawDocumentEntity::getId).orElse(null) : null,
                marketData != null ? ingestionService.getRawDocument(date, "MACRO").map(RawDocumentEntity::getId).orElse(null) : null
            );
            reportRepository.save(reportEntity);
            result.put("report_generated", true);

            // STEP 4: Distribution
            try {
                // Refactor EmailService slightly to take just the Markdown and Date/Bias if possible
                // For now, we adapt to what we have or fix it next
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
            java.util.concurrent.CompletableFuture<Void> macroTask = crawlerService.extract("DailyAnalysis", 1, uuid + "-m");
            java.util.concurrent.CompletableFuture<Void> quantTask = crawlerService.extract("QuantUpdates", 1, uuid + "-q");
            
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
