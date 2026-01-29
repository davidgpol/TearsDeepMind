package com.tearsdeepmind.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tearsdeepmind.domain.model.TemplateEntity;
import com.tearsdeepmind.dto.DailyAnalysisDto;
import com.tearsdeepmind.dto.QuantMemoryDto;
import com.tearsdeepmind.repository.TemplateRepository;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final TemplateRepository templateRepository;
    private final ObjectMapper objectMapper;
    private final GeminiModelsConfiguration geminiModelsConfig;

    @Value("classpath:/prompts/tears-agent-system.st")
    private Resource systemPromptResource;

    @Value("classpath:/prompts/json-mapper.st")
    private Resource jsonMapperPromptResource;

    @Value("${crawler.output.dir:/app/TearsMind}")
    private String outputDir;

    public TearsAgentService(RestClient.Builder restClientBuilder, TemplateRepository templateRepository, ObjectMapper objectMapper, GeminiModelsConfiguration geminiModelsConfig) {
        this.restClient = restClientBuilder.build();
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
        this.geminiModelsConfig = geminiModelsConfig;
    }

    public AnalysisResult generateAnalysis(String date, String rawMarketText, String rawQuantText) {
        try {
            // 1. Generate Report (Markdown)
            String systemPrompt = StreamUtils.copyToString(systemPromptResource.getInputStream(), StandardCharsets.UTF_8);
            String combinedInput = "--- MARKET ANALYSIS ---\n" + rawMarketText + "\n\n--- QUANTLEVELS ---\n" + rawQuantText;
            String fullPrompt = systemPrompt.replace("{input_data}", combinedInput);

            String markdownReport = callGemini(fullPrompt, "markdown_report_gen");

            // 2. Save Markdown
            Path reportPath = saveMarkdownReport(date, markdownReport);

            // 3. Generate Structured Data (JSONs)
            DailyAnalysisDto dailyAnalysis = generateStructuredData(markdownReport, "market_memory", DailyAnalysisDto.class);
            QuantMemoryDto quantMemory = generateStructuredData(markdownReport, "quant_memory", QuantMemoryDto.class);

            return new AnalysisResult(reportPath, dailyAnalysis, quantMemory);
        } catch (IOException e) {
            throw new RuntimeException("Error processing analysis", e);
        }
    }

    private <T> T generateStructuredData(String analysisText, String templateName, Class<T> clazz) throws IOException {
        TemplateEntity templateEntity = templateRepository.findById(templateName)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateName));

        String mapperPromptTemplate = StreamUtils.copyToString(jsonMapperPromptResource.getInputStream(), StandardCharsets.UTF_8);
        String prompt = mapperPromptTemplate
                .replace("{analysis_text}", analysisText)
                .replace("{json_template}", templateEntity.getContent());

        String jsonResponse = callGemini(prompt, "json_mapper_gen");
        jsonResponse = jsonResponse.replaceAll("```json", "").replaceAll("```", "").trim();
        
        // --- Solución Punto 1.1: Mapeo Manual Robusto ---
        Map<String, Object> rawData = objectMapper.readValue(jsonResponse, Map.class);
        String reportDate = (String) rawData.getOrDefault("date", "unknown");

        if (clazz.equals(DailyAnalysisDto.class)) {
            return (T) new DailyAnalysisDto(reportDate, rawData);
        } else if (clazz.equals(QuantMemoryDto.class)) {
            return (T) new QuantMemoryDto(reportDate, rawData);
        }

        return objectMapper.readValue(jsonResponse, clazz);
    }

    protected String callGemini(String promptText, String purpose) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", promptText)))
            )
        );

        // --- Solución Punto 1.2: Fallback de Modelos Corregido y Logging ---
        for (GeminiModelConfig modelConfig : geminiModelsConfig.getModels()) {
            int maxRetries = 3;
            long delayMillis = 1000;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    logger.info("[{}] Intentando llamada a Gemini con modelo [{}] para [{}]... (Intento {}/{})", 
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
                    
                    logger.info("[{}] Éxito obtenido usando el modelo: [{}].", uuid, modelConfig.name());
                    return result;

                } catch (Exception e) {
                    if (e instanceof org.springframework.web.client.HttpClientErrorException.TooManyRequests) {
                        logger.warn("[{}] Modelo [{}] saturado (Error 429). Reintento [{}/{}] en {}ms...", 
                            uuid, modelConfig.name(), attempt, maxRetries, delayMillis);
                        if (attempt < maxRetries) {
                            try { Thread.sleep(delayMillis); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                            delayMillis *= 2;
                        } else {
                            logger.warn("[{}] Modelo [{}] ha agotado todos los reintentos para [{}].", uuid, modelConfig.name(), purpose);
                            // Outer loop continues to next model
                        }
                    } else {
                        logger.error("[{}] El modelo [{}] falló con error: {}. Tipo: {}", 
                            uuid, modelConfig.name(), e.getMessage(), e.getClass().getSimpleName());
                        // Break inner loop to try next model
                        break; 
                    }
                }
            }
            logger.info("[{}] --- ACTIVANDO FALLBACK: Saltando al siguiente modelo en la lista ---", uuid);
        }
        
        logger.fatal("[{}] Se han agotado todos los modelos configurados sin éxito para [{}].", uuid, purpose);
        throw new RuntimeException("Fallo total del pipeline: Todos los modelos de Gemini fallaron o agotaron su cuota.");
    }

    private Path saveMarkdownReport(String date, String content) {
        try {
            String dateFolder = date.replace("-", "");
            Path folder = Paths.get(outputDir, dateFolder);
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }
            Path file = folder.resolve("Trading_Report_" + dateFolder + ".md");
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save markdown report", e);
        }
    }

    public record AnalysisResult(Path reportPath, DailyAnalysisDto dailyAnalysis, QuantMemoryDto quantMemory) {}
}