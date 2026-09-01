package com.tms.controller;

import com.tms.dto.request.WebhookRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.entity.WebhookRegistration;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.WebhookRegistrationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Webhook registration APIs")
public class WebhookController {

    private final WebhookRegistrationRepository webhookRepo;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WebhookRegistration>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(webhookRepo.findAll()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WebhookRegistration>> create(@Valid @RequestBody WebhookRequest req) {
        WebhookRegistration wh = WebhookRegistration.builder()
                .url(req.getUrl()).eventTypes(req.getEventTypes())
                .secret(req.getSecret()).description(req.getDescription()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(webhookRepo.save(wh)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        webhookRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", id));
        webhookRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

