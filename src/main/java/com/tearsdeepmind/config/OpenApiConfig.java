package com.tearsdeepmind.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tearsDeepMindOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("TearsDeepMind API")
                        .description("Platform for SP500 Industrial Crawling & Quantitative Memory")
                        .version("v1.0"));
    }

    @Bean
    public GroupedOpenApi crawlerApi() {
        return GroupedOpenApi.builder()
                .group("1. Crawler Engine")
                .pathsToMatch("/api/v1/crawler/**")
                .build();
    }

    @Bean
    public GroupedOpenApi historyApi() {
        return GroupedOpenApi.builder()
                .group("2. Memory & History")
                .pathsToMatch("/api/history/**")
                .build();
    }
}
