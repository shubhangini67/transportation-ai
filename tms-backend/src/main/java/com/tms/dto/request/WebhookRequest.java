package com.tms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WebhookRequest {
    @NotBlank private String url;
    @NotBlank private String eventTypes;
    private String secret;
    private String description;
}

