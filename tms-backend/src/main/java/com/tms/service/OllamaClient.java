package com.tms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Optional free local LLM via Ollama. No API key. Disabled when URL is blank.
 */
@Component
@Slf4j
public class OllamaClient {

    private final String baseUrl;
    private final String model;
    private final RestTemplate restTemplate;

    public OllamaClient(
            @Value("${app.ai.ollama-url:}") String baseUrl,
            @Value("${app.ai.ollama-model:llama3.2}") String model) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/$", "");
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(12000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return !baseUrl.isBlank();
    }

    public String getModel() {
        return model;
    }

    @SuppressWarnings("unchecked")
    public Optional<String> complete(String prompt) {
        if (!isConfigured()) return Optional.empty();
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false,
                    "options", Map.of("num_predict", 220, "temperature", 0.2)
            );
            Map<String, Object> res = restTemplate.postForObject(baseUrl + "/api/generate", body, Map.class);
            if (res == null || res.get("response") == null) return Optional.empty();
            String text = String.valueOf(res.get("response")).trim();
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (Exception ex) {
            log.debug("Ollama unavailable: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
