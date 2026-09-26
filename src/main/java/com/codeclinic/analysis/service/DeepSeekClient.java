package com.codeclinic.analysis.service;

import com.codeclinic.analysis.config.DeepSeekProperties;
import com.codeclinic.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DeepSeekClient {
    private final DeepSeekProperties properties;
    private final WebClient webClient;

    public DeepSeekClient(DeepSeekProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder.baseUrl(properties.getBaseUrl()).build();
    }

    public String chat(String prompt) {
        if (properties.isMockEnabled()) {
            return mockResponse(prompt);
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException(503, "AI服务未配置访问密钥");
        }
        RuntimeException last = null;
        for (int attempt = 0; attempt <= properties.getMaxRetries(); attempt++) {
            try {
                Map<String, Object> body = Map.of(
                        "model", properties.getModel(),
                        "temperature", 0.2,
                        "response_format", Map.of("type", "json_object"),
                        "messages", List.of(
                                Map.of("role", "system", "content", "你是一名专业的编程教学助手。"),
                                Map.of("role", "user", "content", prompt)));
                JsonNode response = webClient.post()
                        .uri("/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .block();
                JsonNode content = response == null ? null : response.at("/choices/0/message/content");
                if (content == null || content.isMissingNode() || content.asText().isBlank()) {
                    throw new IllegalStateException("模型返回内容为空");
                }
                return content.asText();
            } catch (RuntimeException e) {
                last = e;
                log.warn("AI调用失败，尝试次数={}，异常类型={}", attempt + 1, e.getClass().getSimpleName());
                if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException w
                        && w.getStatusCode().is4xxClientError() && w.getStatusCode().value() != 429) break;
            }
        }
        throw new BusinessException(503, "AI分析服务暂时不可用，请检查服务配置或稍后重试");
    }

    public String modelName() {
        return properties.isMockEnabled() ? properties.getModel() + "-local-demo" : properties.getModel();
    }

    private String safeMessage(Exception exception) {
        return exception == null || exception.getMessage() == null ? "请求失败" : exception.getMessage();
    }

    private String mockResponse(String prompt) {
        if (prompt.contains("arr[3]") || prompt.contains("ArrayIndexOutOfBoundsException")) {
            return """
                    {"errorType":"运行时错误","errorLocation":"arr[3]","errorReason":"数组下标从0开始，长度为3的数组最后一个有效下标是2，访问下标3会超出范围。","suggestion":"将访问位置改为arr[2]；处理动态数组时优先使用arr.length - 1。","correctCode":"int[] arr = {1, 2, 3};\\nSystem.out.println(arr[arr.length - 1]);","knowledgePoints":["Java数组","数组下标","边界检查"],"learningAdvice":"练习根据数组长度推导首尾下标，并在循环条件中使用小于数组长度。"}
                    """;
        }
        return """
                {"errorType":"逻辑错误","errorLocation":"用户提交的代码逻辑","errorReason":"代码的处理过程与期望结果之间存在偏差，需要结合输入边界逐步检查变量变化。","suggestion":"使用最小输入逐行记录关键变量，并将判断条件与题目要求逐项对照。","correctCode":"// 请根据题目约束修正原代码，并为边界输入补充测试。","knowledgePoints":["程序调试","边界条件","测试用例"],"learningAdvice":"先用正常、最小和最大三类输入验证代码，再检查每个分支是否覆盖。"}
                """;
    }
}
