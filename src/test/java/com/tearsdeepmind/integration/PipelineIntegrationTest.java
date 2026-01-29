package com.tearsdeepmind.integration;

import com.tearsdeepmind.domain.model.DailyAnalysisEntity;
import com.tearsdeepmind.dto.DailyAnalysisDto;
import com.tearsdeepmind.dto.QuantMemoryDto;
import com.tearsdeepmind.repository.DailyAnalysisRepository;
import com.tearsdeepmind.service.CrawlerService;
import com.tearsdeepmind.service.PipelineService;
import com.tearsdeepmind.service.TearsAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PipelineIntegrationTest {

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private DailyAnalysisRepository dailyAnalysisRepository;

    @MockBean
    private CrawlerService crawlerService;

    @MockBean
    private TearsAgentService tearsAgentService;

    @Test
    void pipelineShouldSucceedAndPersistData() throws Exception {
        // Arrange
        LocalDate testDate = LocalDate.of(2026, 1, 28);
        when(crawlerService.getReportContent(anyString(), any(LocalDate.class))).thenReturn("Sample Raw Text");
        
        DailyAnalysisDto mockDaily = new DailyAnalysisDto("2026-01-28", Map.of("trend", "bullish"));
        QuantMemoryDto mockQuant = new QuantMemoryDto("2026-01-28", Map.of("support", "6900"));
        
        when(tearsAgentService.generateAnalysis(anyString(), anyString(), anyString()))
            .thenReturn(new TearsAgentService.AnalysisResult(Path.of("/tmp/report.md"), mockDaily, mockQuant));

        // Act
        Map<String, Object> result = pipelineService.runPipeline(testDate);

        // Assert
        assertEquals("SUCCESS", result.get("status"));
        assertTrue(dailyAnalysisRepository.findById("2026-01-28").isPresent());
        
        DailyAnalysisEntity persisted = dailyAnalysisRepository.findById("2026-01-28").get();
        assertNotNull(persisted.getData());
        assertEquals("bullish", persisted.getData().get("trend"));
    }
}