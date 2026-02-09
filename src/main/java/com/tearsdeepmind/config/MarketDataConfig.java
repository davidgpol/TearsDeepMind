package com.tearsdeepmind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MarketDataConfig {

    @Bean
    public RestClient yahooFinanceClient() {
        return RestClient.builder()
                .baseUrl("https://query1.finance.yahoo.com")
                .build();
    }
}
