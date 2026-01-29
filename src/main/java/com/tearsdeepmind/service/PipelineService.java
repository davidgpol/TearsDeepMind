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
            // 1. Data Acquisition
            String marketText = crawlerService.getReportContent("DailyAnalysis", date);
            String quantText = crawlerService.getReportContent("QuantUpdates", date);

            if (marketText == null || quantText == null) {
                throw new RuntimeException("Reports not found for date: " + date + ". Please run crawler first.");
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
