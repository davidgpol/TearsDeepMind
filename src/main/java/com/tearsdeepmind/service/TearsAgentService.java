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

    @Value("classpath:/prompts/post-mortem-v1.st")
    private Resource postMortemPromptResource;

    public TearsAgentService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, GeminiModelsConfiguration geminiModelsConfig) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.geminiModelsConfig = geminiModelsConfig;
    }

    public String generatePostMortem(String predictedDirection, String actualDirection, double dailyChangePct, String technicalContext) {
        try {
            String promptTemplate = StreamUtils.copyToString(postMortemPromptResource.getInputStream(), StandardCharsets.UTF_8);
            String fullPrompt = promptTemplate
                    .replace("{predicted_direction}", predictedDirection)
                    .replace("{actual_direction}", actualDirection)
                    .replace("{daily_change_pct}", String.valueOf(dailyChangePct))
                    .replace("{technical_context}", technicalContext != null ? technicalContext : "Unknown");
            
            return callGemini(fullPrompt, "post_mortem_analysis");
        } catch (IOException e) {
            logger.error("Error generating post mortem", e);
            return "Failed to generate post mortem due to technical error.";
        }
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

    public MarketMemoryRecord extractMacroIntelligence(String rawText, String technicalContext, String cognitiveMemory) {
        try {
            String promptTemplate = StreamUtils.copyToString(macroPromptResource.getInputStream(), StandardCharsets.UTF_8);
            String fullPrompt = promptTemplate
                    .replace("{raw_macro_data}", rawText)
                    .replace("{technical_context}", technicalContext != null ? technicalContext : "No technical data available.")
                    .replace("{cognitive_memory}", cognitiveMemory != null ? cognitiveMemory : "No recent errors.");
            
            String jsonResponse = callGemini(fullPrompt, "macro_extraction");
            
            // Robust parsing logic
            JsonNode root = objectMapper.readTree(jsonResponse);
            
            // Extract simple fields
            String dateStr = root.path("date").asText("").trim();
            LocalDate date = null;
            try {
                if (!dateStr.isEmpty()) {
                    date = LocalDate.parse(dateStr);
                }
            } catch (Exception e) {
                logger.warn("Could not parse date '{}' from Gemini response, using null", dateStr);
            }
            String sourceTitle = root.path("source_title").asText("");
            
            // Extract complex objects safely
            MarketMemoryRecord.SentimentProfile sentiment = objectMapper.treeToValue(root.path("sentiment_profile"), MarketMemoryRecord.SentimentProfile.class);
            MarketMemoryRecord.Drivers drivers = objectMapper.treeToValue(root.path("drivers"), MarketMemoryRecord.Drivers.class);
            MarketMemoryRecord.DailyThesis thesis = objectMapper.treeToValue(root.path("daily_thesis"), MarketMemoryRecord.DailyThesis.class);
            MarketMemoryRecord.NarrativeLevels levels = objectMapper.treeToValue(root.path("narrative_levels"), MarketMemoryRecord.NarrativeLevels.class);

            // Manual parsing for StructuredPrediction to handle Doubles safely
            JsonNode spNode = root.path("structured_prediction");
            MarketMemoryRecord.StructuredPrediction prediction = null;
            if (!spNode.isMissingNode()) {
                String squeezeStatus = spNode.path("squeeze_status").asText("INACTIVE");
                String direction = spNode.path("direction").asText("FLAT");
                String volatility = spNode.path("volatility").asText("STABLE");
                
                Double rangeTop = spNode.path("expected_range_top").isNumber() ? spNode.path("expected_range_top").asDouble() : null;
                Double rangeBottom = spNode.path("expected_range_bottom").isNumber() ? spNode.path("expected_range_bottom").asDouble() : null;
                Double target = spNode.path("primary_target").isNumber() ? spNode.path("primary_target").asDouble() : null;
                
                prediction = new MarketMemoryRecord.StructuredPrediction(squeezeStatus, direction, volatility, rangeTop, rangeBottom, target);
            }

            return new MarketMemoryRecord(date, sourceTitle, sentiment, prediction, drivers, thesis, levels);

        } catch (IOException e) {
            throw new RuntimeException("Error extracting macro intelligence", e);
        }
    }

    public String generateFinalReport(LocalDate date, QuantMemoryRecord quantData, MarketMemoryRecord marketData, 
                                     String marketReality, String auditVerdict, String turboStrategyBlock) {
        try {
            String promptTemplate = StreamUtils.copyToString(reportPromptResource.getInputStream(), StandardCharsets.UTF_8);
            
            String quantJson = (quantData != null) ? objectMapper.writeValueAsString(quantData) : "{\"status\": \"DATA_NOT_AVAILABLE\"}";
            String marketJson = (marketData != null) ? objectMapper.writeValueAsString(marketData) : "{\"status\": \"DATA_NOT_AVAILABLE\"}";

            String fullPrompt = promptTemplate
                    .replace("{date}", date.toString())
                    .replace("{quant_json}", quantJson)
                    .replace("{market_json}", marketJson)
                    .replace("{market_reality}", marketReality != null ? marketReality : "No market data available.")
                    .replace("{audit_verdict}", auditVerdict != null ? auditVerdict : "No previous audit data.")
                    .replace("{turbo_strategy_block}", turboStrategyBlock != null ? turboStrategyBlock : "No turbo strategy available.");

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