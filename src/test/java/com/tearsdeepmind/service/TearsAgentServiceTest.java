package com.tearsdeepmind.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tearsdeepmind.config.GeminiModelsConfiguration;
import com.tearsdeepmind.config.GeminiModelsConfiguration.GeminiModelConfig;
import com.tearsdeepmind.domain.model.TemplateEntity;
import com.tearsdeepmind.dto.DailyAnalysisDto;
import com.tearsdeepmind.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TearsAgentServiceTest {

    private TearsAgentService tearsAgentService;
    private RestClient restClient;
    private TemplateRepository templateRepository;
    private ObjectMapper objectMapper;
    private GeminiModelsConfiguration geminiModelsConfig;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        restClient = mock(RestClient.class);
        templateRepository = mock(TemplateRepository.class);
        objectMapper = new ObjectMapper();
        geminiModelsConfig = mock(GeminiModelsConfiguration.class);
        when(builder.build()).thenReturn(restClient);
        tearsAgentService = new TearsAgentService(builder, templateRepository, objectMapper, geminiModelsConfig);
    }

    @Test
    void shouldMapFlatJsonToNestedDtoCorrecty() throws Exception {
        // Mocking only the mapping logic (Point 1.1)
        String flatJson = "{\"date\": \"2026-01-28\", \"regime\": {\"trend\": \"bullish\"}}";
        
        // Setup internal resources
        Resource mockMapperPrompt = mock(Resource.class);
        when(mockMapperPrompt.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));
        java.lang.reflect.Field mapperField = TearsAgentService.class.getDeclaredField("jsonMapperPromptResource");
        mapperField.setAccessible(true);
        mapperField.set(tearsAgentService, mockMapperPrompt);
        
        TemplateEntity mockTemplate = new TemplateEntity();
        mockTemplate.setContent("{}");
        when(templateRepository.findById(anyString())).thenReturn(Optional.of(mockTemplate));

        // Mock callGemini via reflection or partial mock
        TearsAgentService spyService = spy(tearsAgentService);
        doReturn(flatJson).when(spyService).callGemini(anyString(), anyString());

        // Act
        java.lang.reflect.Method method = TearsAgentService.class.getDeclaredMethod("generateStructuredData", String.class, String.class, Class.class);
        method.setAccessible(true);
        DailyAnalysisDto result = (DailyAnalysisDto) method.invoke(spyService, "some analysis", "market_memory", DailyAnalysisDto.class);

        // Assert
        assertNotNull(result);
        assertEquals("2026-01-28", result.date());
        assertNotNull(result.data());
        assertTrue(result.data().containsKey("regime"));
    }
}
