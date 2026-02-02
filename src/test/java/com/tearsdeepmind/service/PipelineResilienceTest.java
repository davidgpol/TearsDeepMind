package com.tearsdeepmind.service;

import com.tearsdeepmind.dto.DailyAnalysisDto;
import com.tearsdeepmind.dto.QuantMemoryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PipelineResilienceTest {

    @Mock
    private CrawlerService crawlerService;
    @Mock
    private TearsAgentService tearsAgentService;
    @Mock
    private HistoryService historyService;
    @Mock
    private EmailService emailService;

    private PipelineService pipelineService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pipelineService = new PipelineService(crawlerService, tearsAgentService, historyService, emailService);
    }

    @Test
    void runPipeline_ShouldSucceedEvenIfEmailFails() throws java.io.IOException {
        // Arrange
        LocalDate date = LocalDate.of(2026, 1, 30);
        String dateStr = date.toString();

        when(crawlerService.getReportContent(anyString(), eq(date))).thenReturn("some content");
        
        TearsAgentService.AnalysisResult mockResult = new TearsAgentService.AnalysisResult(
                Path.of("temp.md"),
                new DailyAnalysisDto(dateStr, Map.of("Bias", "NEUTRAL")),
                new QuantMemoryDto(dateStr, Map.of())
        );
        when(tearsAgentService.generateAnalysis(anyString(), anyString(), anyString())).thenReturn(mockResult);

        // Simulate Email Service Failure
        doThrow(new RuntimeException("SMTP Server Down")).when(emailService).sendReportWithAttachment(any(), any());

        // Act
        Map<String, Object> result = pipelineService.runPipeline(date);

        // Assert
        assertEquals("SUCCESS", result.get("status"));
        assertEquals(false, result.get("notification_sent"));
        assertEquals("SMTP Server Down", result.get("notification_error"));
        
        // Verify other steps completed
        verify(historyService).saveDailyAnalysis(eq(dateStr), any());
        verify(historyService).saveQuantMemory(eq(dateStr), any());
    }
}
