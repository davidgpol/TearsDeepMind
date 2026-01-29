package com.tearsdeepmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "gemini")
public class GeminiModelsConfiguration {

    private List<GeminiModelConfig> models;

    public List<GeminiModelConfig> getModels() {
        return models;
    }

    public void setModels(List<GeminiModelConfig> models) {
        this.models = models;
    }

    public static record GeminiModelConfig(String name, String url, String key) {
    }
}
