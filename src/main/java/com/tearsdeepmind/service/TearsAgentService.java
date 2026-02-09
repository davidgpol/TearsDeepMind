package com.tearsdeepmind.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tearsdeepmind.domain.model.MarketMemoryRecord;
import com.tearsdeepmind.domain.model.QuantMemoryRecord;
import com.tearsdeepmind.config.GeminiModelsConfiguration;
import com.tearsdeepmind.config.GeminiModelsConfiguration.GeminiModelConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
@EnableConfigurationProperties(GeminiModelsConfiguration.class)
public class TearsAgentService {

    private static final Logger logger = LogManager.getLogger(TearsAgentService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GeminiModelsConfiguration geminiModelsConfig;

    @Value("classpath:/prompts/quant-extractor-v1.st")
    private Resource quantPromptResource;

    @Value("classpath:/prompts/macro-extractor-v1.st")
    private Resource macroPromptResource;

    @Value("classpath:/prompts/report-generator-v1.st")
    private Resource reportPromptResource;

    public TearsAgentService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, GeminiModelsConfiguration geminiModelsConfig) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.geminiModelsConfig = geminiModelsConfig;
    }

    public QuantMemoryRecord extractQuantIntelligence(String rawText) {
        try {
            String promptTemplate = StreamUtils.copyToString(quantPromptResource.getInputStream(), StandardCharsets.UTF_8);
            String fullPrompt = promptTemplate.replace("{raw_quant_data}", rawText);
            String jsonResponse = callGemini(fullPrompt, "quant_extraction");
            return objectMapper.readValue(jsonResponse, QuantMemoryRecord.class);
        } catch (IOException e) {
            throw new RuntimeException("Error extracting quant intelligence", e);
        }
    }

    public MarketMemoryRecord extractMacroIntelligence(String rawText) {
        try {
            String promptTemplate = StreamUtils.copyToString(macroPromptResource.getInputStream(), StandardCharsets.UTF_8);
            String fullPrompt = promptTemplate.replace("{raw_macro_data}", rawText);
            String jsonResponse = callGemini(fullPrompt, "macro_extraction");
            return objectMapper.readValue(jsonResponse, MarketMemoryRecord.class);
        } catch (IOException e) {
            throw new RuntimeException("Error extracting macro intelligence", e);
        }
    }

    public String generateFinalReport(LocalDate date, QuantMemoryRecord quantData, MarketMemoryRecord marketData, 
                                     String marketReality, String auditVerdict) {
        try {
            String promptTemplate = StreamUtils.copyToString(reportPromptResource.getInputStream(), StandardCharsets.UTF_8);
            
            String quantJson = (quantData != null) ? objectMapper.writeValueAsString(quantData) : "{\"status\": \"DATA_NOT_AVAILABLE\"}";
            String marketJson = (marketData != null) ? objectMapper.writeValueAsString(marketData) : "{\"status\": \"DATA_NOT_AVAILABLE\"}";

            String fullPrompt = promptTemplate
                    .replace("{date}", date.toString())
                    .replace("{quant_json}", quantJson)
                    .replace("{market_json}", marketJson)
                    .replace("{market_reality}", marketReality != null ? marketReality : "No market data available.")
                    .replace("{audit_verdict}", auditVerdict != null ? auditVerdict : "No previous audit data.");

            return callGemini(fullPrompt, "report_generation");
        } catch (IOException e) {
            throw new RuntimeException("Error generating final report", e);
        }
    }

    protected String callGemini(String promptText, String purpose) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", promptText)))
            )
        );

        for (GeminiModelConfig modelConfig : geminiModelsConfig.getModels()) {
            int maxRetries = 3;
            long delayMillis = 1000;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    logger.info("[{}] Attempting Gemini call with model [{}] for [{}]... (Attempt {}/{})", 
                        uuid, modelConfig.name(), purpose, attempt, maxRetries);
                    
                    String uri = modelConfig.url() + "?key=" + modelConfig.key();
                    String response = restClient.post()
                            .uri(uri)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestBody)
                            .retrieve()
                            .body(String.class);
        
                    JsonNode root = objectMapper.readTree(response);
                    String result = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                    
                    // Cleanup potential markdown blocks
                    result = result.replaceAll("```json", "").replaceAll("```", "").trim();
                    
                    logger.info("[{}] Success using model: [{}].", uuid, modelConfig.name());
                    return result;

                } catch (Exception e) {
                    if (e instanceof org.springframework.web.client.HttpClientErrorException.TooManyRequests) {
                        logger.warn("[{}] Model [{}] saturated (Error 429). Retry [{}/{}] in {}ms...", 
                            uuid, modelConfig.name(), attempt, maxRetries, delayMillis);
                        if (attempt < maxRetries) {
                            try { Thread.sleep(delayMillis); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                            delayMillis *= 2;
                        }
                    } else {
                        logger.error("[{}] Model [{}] failed with error: {}.", uuid, modelConfig.name(), e.getMessage());
                        break; 
                    }
                }
            }
        }
        throw new RuntimeException("All Gemini models failed for " + purpose);
    }
}