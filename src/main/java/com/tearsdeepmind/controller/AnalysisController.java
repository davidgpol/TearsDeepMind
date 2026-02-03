package com.tearsdeepmind.controller;

import com.tearsdeepmind.repository.DailyAnalysisRepository;
import com.tearsdeepmind.repository.QuantMemoryRepository;
import com.tearsdeepmind.repository.ReportRepository;
import com.tearsdeepmind.service.IngestionService;
import com.tearsdeepmind.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/analysis")
@Tag(name = "Analysis & Lifecycle", description = "Endpoints for diagnostic and manual reprocessing of intelligence data.")
public class AnalysisController {

    private final PipelineService pipelineService;
    private final IngestionService ingestionService;
    private final DailyAnalysisRepository dailyRepository;
    private final QuantMemoryRepository quantRepository;
    private final ReportRepository reportRepository;

    public AnalysisController(PipelineService pipelineService, IngestionService ingestionService, DailyAnalysisRepository dailyRepository, QuantMemoryRepository quantRepository, ReportRepository reportRepository) {
        this.pipelineService = pipelineService;
        this.ingestionService = ingestionService;
        this.dailyRepository = dailyRepository;
        this.quantRepository = quantRepository;
        this.reportRepository = reportRepository;
    }

    @Operation(summary = "Get Day Status", description = "Checks availability of Raw, JSON and Report for a specific date.")
    @GetMapping("/status/{date}")
    public Map<String, Object> getStatus(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        return Map.of(
            "date", date,
            "raw_macro", ingestionService.getRawDocument(localDate, "MACRO").isPresent(),
            "raw_quant", ingestionService.getRawDocument(localDate, "QUANT").isPresent(),
            "analysis_macro", dailyRepository.findById(localDate).isPresent(),
            "analysis_quant", quantRepository.findById(localDate).isPresent(),
            "report_final", reportRepository.findById(localDate).isPresent()
        );
    }

    @Operation(summary = "Regenerate Day", description = "Deletes existing analysis and re-runs the pipeline from existing raw sources.")
    @PostMapping("/regenerate/{date}")
    public Map<String, Object> regenerate(@PathVariable String date) {
        LocalDate localDate = LocalDate.now();
        dailyRepository.deleteById(localDate);
        quantRepository.deleteById(localDate);
        return pipelineService.runPipeline(localDate);
    }
}
