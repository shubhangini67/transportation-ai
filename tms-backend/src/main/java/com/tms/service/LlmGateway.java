package com.tms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Optional cloud/local LLMs. First configured provider wins.
 * Grok (xAI) → Gemini → Groq → Ollama. Demo works with none of these set.
 */
@Component
@Slf4j
public class LlmGateway {

    private final String grokKey;
    private final String grokModel;
    private final String geminiKey;
    private final String geminiModel;
    private final String groqKey;
    private final String groqModel;
    private final OllamaClient ollamaClient;
    private final RestTemplate restTemplate;

    public LlmGateway(
            @Value("${app.ai.grok-api-key:}") String grokKey,
            @Value("${app.ai.grok-model:grok-3-mini}") String grokModel,
            @Value("${app.ai.gemini-api-key:}") String geminiKey,
            @Value("${app.ai.gemini-model:gemini-2.0-flash}") String geminiModel,
            @Value("${app.ai.groq-api-key:}") String groqKey,
            @Value("${app.ai.groq-model:llama-3.3-70b-versatile}") String groqModel,
            OllamaClient ollamaClient) {
        this.grokKey = blankToEmpty(grokKey);
        this.grokModel = grokModel;
        this.geminiKey = blankToEmpty(geminiKey);
        this.geminiModel = geminiModel;
        this.groqKey = blankToEmpty(groqKey);
        this.groqModel = groqModel;
        this.ollamaClient = ollamaClient;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(14000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return !grokKey.isBlank() || !geminiKey.isBlank() || !groqKey.isBlank() || ollamaClient.isConfigured();
    }

    public String engineName(boolean usedLlm) {
        if (!usedLlm) {
            return isConfigured()
                    ? "Live RAG (LLM key set but call fell back to facts)"
                    : "Live RAG copilot (no API key — still answers from TMS data)";
        }
        return activeProviderLabel() + " grounded on live TMS facts";
    }

    public String activeProviderLabel() {
        if (!grokKey.isBlank()) return "Grok / xAI (" + grokModel + ")";
        if (!geminiKey.isBlank()) return "Gemini (" + geminiModel + ")";
        if (!groqKey.isBlank()) return "Groq (" + groqModel + ")";
        if (ollamaClient.isConfigured()) return "Ollama " + ollamaClient.getModel();
        return "Live RAG (no LLM)";
    }

    public Optional<String> complete(String prompt) {
        if (!grokKey.isBlank()) {
            Optional<String> grok = openAiChat("https://api.x.ai/v1/chat/completions", grokKey, grokModel, prompt);
            if (grok.isPresent()) return grok;
        }
        if (!geminiKey.isBlank()) {
            Optional<String> gemini = geminiChat(prompt);
            if (gemini.isPresent()) return gemini;
        }
        if (!groqKey.isBlank()) {
            for (String model : groqModels()) {
                Optional<String> groq = openAiChat("https://api.groq.com/openai/v1/chat/completions", groqKey, model, prompt);
                if (groq.isPresent()) return groq;
            }
        }
        if (ollamaClient.isConfigured()) {
            return ollamaClient.complete(prompt);
        }
        return Optional.empty();
    }

    private List<String> groqModels() {
        List<String> models = new java.util.ArrayList<>();
        models.add(groqModel);
        for (String fallback : List.of("openai/gpt-oss-20b", "openai/gpt-oss-120b", "llama-3.3-70b-versatile")) {
            if (!models.contains(fallback)) models.add(fallback);
        }
        return models;
    }

    @SuppressWarnings("unchecked")
    private Optional<String> openAiChat(String url, String apiKey, String model, String prompt) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("temperature", 0.15);
            body.put("max_tokens", 500);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", """
                            You are TMS Bot, the operations assistant for this transport fleet.
                            Answer the user's actual question. Greet back when they say hi, hello, or good evening.
                            Only dump a shift handover when they asked for hi, briefing, or status.
                            Never mention Groq, OpenAI, Gemini, Ollama, API keys, or model names.
                            Never invent vehicle numbers, drivers, or counts.
                            """),
                    Map.of("role", "user", "content", prompt)
            ));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            ResponseEntity<Map> res = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
            Map<String, Object> json = res.getBody();
            if (json == null) return Optional.empty();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) json.get("choices");
            if (choices == null || choices.isEmpty()) return Optional.empty();
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null || message.get("content") == null) return Optional.empty();
            String text = String.valueOf(message.get("content")).trim();
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (Exception ex) {
            log.warn("LLM {} model {} unavailable: {}", url, model, ex.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<String> geminiChat(String prompt) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + geminiModel + ":generateContent?key=" + geminiKey;
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", 0.15, "maxOutputTokens", 500)
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> res = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
            Map<String, Object> json = res.getBody();
            if (json == null) return Optional.empty();
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) json.get("candidates");
            if (candidates == null || candidates.isEmpty()) return Optional.empty();
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return Optional.empty();
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty() || parts.get(0).get("text") == null) return Optional.empty();
            String text = String.valueOf(parts.get(0).get("text")).trim();
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (Exception ex) {
            log.info("Gemini unavailable: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
