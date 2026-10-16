package com.codeclinic.analysis.service;

import com.codeclinic.analysis.config.DeepSeekProperties;
import com.codeclinic.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeepSeekClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void localOpenAiCompatibleModelWorksWithoutApiKey() throws IOException {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"{\\\"errorType\\\":\\\"运行时错误\\\"}\"}}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        DeepSeekProperties properties = properties("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        DeepSeekClient client = new DeepSeekClient(properties, WebClient.builder());

        assertThat(client.chat("分析这段代码")).contains("运行时错误");
        assertThat(authorization.get()).isNull();
        assertThat(requestBody.get()).contains("qwen3:8b", "分析这段代码", "json_object");
        assertThat(client.modelName()).isEqualTo("qwen3:8b");
    }

    @Test
    void remoteModelRequiresApiKey() {
        DeepSeekClient client = new DeepSeekClient(properties("https://api.deepseek.com"), WebClient.builder());

        assertThatThrownBy(() -> client.chat("分析这段代码"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI服务未配置访问密钥");
    }

    @Test
    void onlyLoopbackAddressIsTreatedAsLocal() {
        DeepSeekClient local = new DeepSeekClient(properties("http://localhost:11434/v1"), WebClient.builder());
        DeepSeekClient remote = new DeepSeekClient(properties("https://models.example.com/v1"), WebClient.builder());

        assertThat(local.isLocalEndpoint()).isTrue();
        assertThat(remote.isLocalEndpoint()).isFalse();
    }

    private DeepSeekProperties properties(String baseUrl) {
        DeepSeekProperties properties = new DeepSeekProperties();
        properties.setBaseUrl(baseUrl);
        properties.setModel("qwen3:8b");
        properties.setMockEnabled(false);
        properties.setTimeoutSeconds(5);
        properties.setMaxRetries(0);
        return properties;
    }
}
