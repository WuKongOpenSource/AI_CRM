package com.kakarote.ai_crm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "system-ai")
public class SystemAiModelProperties {

    private Map<String, Provider> providers = new LinkedHashMap<>();

    @Data
    public static class Provider {

        private Boolean enabled = true;

        private String baseUrl;

        private String apiKey;

        private String model;

        private Double temperature;

        private Integer maxTokens;

        private String extraHeadersJson;
    }
}
