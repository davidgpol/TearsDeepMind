package com.tearsdeepmind.service;

import com.tearsdeepmind.dto.DailyAnalysisDto;
import com.tearsdeepmind.dto.QuantMemoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class PipelineService {

    private static final Logger logger = LoggerFactory.getLogger(PipelineService.class);

    private final CrawlerService crawlerService;
    private final TearsAgentService tearsAgentService;
    private final HistoryService historyService;
    private final EmailService emailService;

    public PipelineService(CrawlerService crawlerService, TearsAgentService tearsAgentService, HistoryService historyService, EmailService emailService) {
        this.crawlerService = crawlerService;
        this.tearsAgentService = tearsAgentService;
        this.historyService = historyService;
        this.emailService = emailService;
    }

    public Map<String, Object> runPipeline(LocalDate date) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. Data Acquisition (Check & Download)
            String marketText = crawlerService.getReportContent("DailyAnalysis", date);
            String quantText = crawlerService.getReportContent("QuantUpdates", date);

            if (marketText == null || quantText == null) {
                String uuid = java.util.UUID.randomUUID().toString();
                
                java.util.concurrent.CompletableFuture<Void> marketFuture = crawlerService.extract("DailyAnalysis", 3, uuid + "-market");
                java.util.concurrent.CompletableFuture<Void> quantFuture = crawlerService.extract("QuantUpdates", 3, uuid + "-quant");
                
                java.util.concurrent.CompletableFuture.allOf(marketFuture, quantFuture).join();

                marketText = crawlerService.getReportContent("DailyAnalysis", date);
                quantText = crawlerService.getReportContent("QuantUpdates", date);
            }

            if (marketText == null || quantText == null) {
                throw new RuntimeException("Reports not found for date: " + date + " even after crawler execution.");
            }

            // 2. Intelligence & Analysis
            TearsAgentService.AnalysisResult analysis = tearsAgentService.generateAnalysis(date.toString(), marketText, quantText);
            result.put("report_path", analysis.reportPath().toString());

            // 3. Persistence
            DailyAnalysisDto dailyDto = analysis.dailyAnalysis();
            historyService.saveDailyAnalysis(date.toString(), dailyDto.data());
            
            QuantMemoryDto quantDto = analysis.quantMemory();
            historyService.saveQuantMemory(date.toString(), quantDto.data());

            // 4. Notification (Resilient Step)
            try {
                logger.info("Triggering email notification for date: {}", date);
                emailService.sendReportWithAttachment(analysis.reportPath().toFile(), dailyDto);
                result.put("notification_sent", true);
            } catch (Exception e) {
                logger.error("Email notification failed for date: {}. Error: {}", date, e.getMessage());
                result.put("notification_sent", false);
                result.put("notification_error", e.getMessage());
                // We do NOT rethrow here to protect the pipeline result
            }

            result.put("status", "SUCCESS");
            result.put("persisted_market", true);
            result.put("persisted_quant", true);

        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            throw new RuntimeException("Pipeline failed", e);
        }
        return result;
    }
}