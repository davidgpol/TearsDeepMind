package com.tearsdeepmind.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tearsdeepmind.config.GeminiModelsConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootTest
@ActiveProfiles("test")
public class QuantReprocessor {

    private static final Logger logger = LoggerFactory.getLogger(QuantReprocessor.class);

    @Autowired
    private GeminiModelsConfiguration geminiConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Value("classpath:/prompts/quant-extractor-v1.st")
    private Resource promptResource;

    @MockBean
    private JavaMailSender javaMailSender;

    // Hardcoded path to the volume for this utility script
    private final String volumePath = "../../Volumes/TearsMind";

    @Test
    public void reprocessLast5Days() throws Exception {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(5);
        RestClient restClient = restClientBuilder.build();
        String promptTemplate = StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);

        // Iterate dates
        start.datesUntil(end.plusDays(1)).forEach(date -> {
            String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
            Path dayFolder = Paths.get(volumePath, dateStr);
            Path quantFolder = dayFolder.resolve("QuantUpdates");

            if (!Files.exists(quantFolder)) {
                logger.warn("Skipping {}: QuantUpdates folder not found.", dateStr);
                return;
            }

            try (Stream<Path> files = Files.list(quantFolder)) {
                Path txtFile = files.filter(p -> p.toString().endsWith(".txt")).findFirst().orElse(null);
                
                if (txtFile == null) {
                    logger.warn("Skipping {}: No .txt file in QuantUpdates.", dateStr);
                    return;
                }

                logger.info("Processing file: {}", txtFile);
                String content = Files.readString(txtFile);
                String fullPrompt = promptTemplate.replace("{input_text}", content);

                // Call Gemini (Simplified Logic for Utility Script)
                String jsonResult = callGemini(restClient, fullPrompt);
                
                // Save JSON to QuantUpdates folder with specific filename
                Path outputPath = quantFolder.resolve("quant_memory_" + dateStr + ".json");
                Files.writeString(outputPath, jsonResult);
                logger.info("Saved JSON to: {}", outputPath);

            } catch (IOException e) {
                logger.error("Error processing date " + dateStr, e);
            }
        });
    }

    private String callGemini(RestClient restClient, String prompt) {
        // Use the first configured model for this test
        var modelConfig = geminiConfig.getModels().get(0);
        String url = modelConfig.url() + "?key=" + modelConfig.key();

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            )
        );

        String response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            // Clean markdown code blocks if present
            return text.replaceAll("```json", "").replaceAll("```", "").trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }
}
