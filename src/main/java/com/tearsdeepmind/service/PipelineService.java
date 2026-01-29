package com.tearsdeepmind.service;

import com.tearsdeepmind.dto.DailyAnalysisDto;
import com.tearsdeepmind.dto.QuantMemoryDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class PipelineService {

    private final CrawlerService crawlerService;
    private final TearsAgentService tearsAgentService;
    private final HistoryService historyService;

    public PipelineService(CrawlerService crawlerService, TearsAgentService tearsAgentService, HistoryService historyService) {
        this.crawlerService = crawlerService;
        this.tearsAgentService = tearsAgentService;
        this.historyService = historyService;
    }

    public Map<String, Object> runPipeline(LocalDate date) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. Data Acquisition (Check & Download)
            String marketText = crawlerService.getReportContent("DailyAnalysis", date);
            String quantText = crawlerService.getReportContent("QuantUpdates", date);

            if (marketText == null || quantText == null) {
                // If reports are missing, trigger download synchronously
                // We request 1 day of lookback, targeting the specific date implicitly by fetching recent posts
                // Note: The crawler fetches the latest posts. If the target date is older than the latest batch, this might need adjustment.
                // But for daily runs (today), fetching "1 day" or "2 days" of recent posts is sufficient.
                String uuid = java.util.UUID.randomUUID().toString();
                
                // Trigger downloads in parallel and wait
                java.util.concurrent.CompletableFuture<Void> marketFuture = crawlerService.extract("DailyAnalysis", 3, uuid + "-market");
                java.util.concurrent.CompletableFuture<Void> quantFuture = crawlerService.extract("QuantUpdates", 3, uuid + "-quant");
                
                java.util.concurrent.CompletableFuture.allOf(marketFuture, quantFuture).join();

                // Re-fetch content after download
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
            // Ensure date consistency if LLM hallucinated the date field
            historyService.saveDailyAnalysis(date.toString(), dailyDto.data());
            
            QuantMemoryDto quantDto = analysis.quantMemory();
            historyService.saveQuantMemory(date.toString(), quantDto.data());

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
