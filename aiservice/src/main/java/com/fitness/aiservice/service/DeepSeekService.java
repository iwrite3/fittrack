package com.fitness.aiservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService {
    private final WebClient webClient;

    @Value("${deepseek.api.url}")
    private String apiUrl;
    @Value("${deepseek.api.key}")
    private String apiKey;
    @Value("${deepseek.api.model}")
    private String model;

    public DeepSeekService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /** Sends the prompt to DeepSeek and returns the raw JSON response (OpenAI-style: choices[0].message.content). */
    public String getAnswer(String question) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", question)),
                "response_format", Map.of("type", "json_object"),
                "stream", false
        );

        return webClient.post()
                .uri(apiUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), r -> r.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new RuntimeException("DeepSeek API error " + r.statusCode() + ": " + body)))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(3))
                        .filter(e -> e.getMessage() != null
                                && (e.getMessage().contains("503") || e.getMessage().contains("429")
                                || e.getMessage().contains("500")))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .block();
    }
}
