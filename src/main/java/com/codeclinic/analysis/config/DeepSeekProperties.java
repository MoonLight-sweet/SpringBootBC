package com.codeclinic.analysis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.deepseek")
public class DeepSeekProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
    private boolean mockEnabled;
    private int timeoutSeconds;
    private int maxRetries;
}
