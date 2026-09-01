package com.tms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.entity.WebhookRegistration;
import com.tms.enums.WebhookEventType;
import com.tms.repository.WebhookRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatchService {

    private final WebhookRegistrationRepository webhookRepo;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void dispatch(WebhookEventType eventType, Object payload) {
        List<WebhookRegistration> registrations = webhookRepo.findByActiveTrue();
        for (WebhookRegistration reg : registrations) {
            if (reg.getEventTypes().contains(eventType.name())) {
                try {
                    sendWebhook(reg, eventType, payload);
                } catch (Exception e) {
                    log.warn("Webhook delivery failed for {} to {}: {}", eventType, reg.getUrl(), e.getMessage());
                }
            }
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendWebhook(WebhookRegistration reg, WebhookEventType eventType, Object payload) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("event", eventType.name());
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("data", payload);

            String json = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (reg.getSecret() != null && !reg.getSecret().isBlank()) {
                headers.set("X-Webhook-Signature", hmacSha256(json, reg.getSecret()));
            }

            HttpEntity<String> request = new HttpEntity<>(json, headers);
            ResponseEntity<String> response = restTemplate.exchange(reg.getUrl(), HttpMethod.POST, request, String.class);
            log.info("Webhook delivered: {} → {} ({})", eventType, reg.getUrl(), response.getStatusCode());
        } catch (Exception e) {
            log.error("Webhook error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }
}

