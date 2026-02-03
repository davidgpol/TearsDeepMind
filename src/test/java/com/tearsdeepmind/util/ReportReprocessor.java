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

@SpringBootTest
@ActiveProfiles("test")
public class ReportReprocessor {

    private static final Logger logger = LoggerFactory.getLogger(ReportReprocessor.class);

    @Autowired
    private GeminiModelsConfiguration geminiConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Value("classpath:/prompts/report-generator-v1.st")
    private Resource promptResource;

    @MockBean
    private JavaMailSender javaMailSender;

    private final String volumePath = "../../Volumes/TearsMind";

    @Test
    public void generateReportsLast5Days() throws Exception {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(5);
        RestClient restClient = restClientBuilder.build();
        String promptTemplate = StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);

        start.datesUntil(end.plusDays(1)).forEach(date -> {
            String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
            String dashedDate = date.toString(); // YYYY-MM-DD
            Path dayFolder = Paths.get(volumePath, dateStr);
            
            Path quantJsonPath = dayFolder.resolve("QuantUpdates").resolve("quant_memory_" + dateStr + ".json");
            Path marketJsonPath = dayFolder.resolve("DailyAnalysis").resolve("market_memory_" + dateStr + ".json");

            if (!Files.exists(quantJsonPath) || !Files.exists(marketJsonPath)) {
                logger.warn("Skipping {}: Missing JSON files.", dateStr);
                return;
            }

            try {
                logger.info("Generating report for: {}", dateStr);
                String quantJson = Files.readString(quantJsonPath);
                String marketJson = Files.readString(marketJsonPath);

                String fullPrompt = promptTemplate
                        .replace("{date}", dashedDate)
                        .replace("{quant_json}", quantJson)
                        .replace("{market_json}", marketJson);

                String reportMarkdown = callGemini(restClient, fullPrompt);
                
                Path outputPath = dayFolder.resolve("Trading_Report_v2.md");
                Files.writeString(outputPath, reportMarkdown);
                logger.info("Saved Report to: {}", outputPath);

            } catch (IOException e) {
                logger.error("Error generating report for " + dateStr, e);
            }
        });
    }

    private String callGemini(RestClient restClient, String prompt) {
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
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }
}
