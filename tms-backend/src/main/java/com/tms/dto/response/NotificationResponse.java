package com.tms.dto.response;

import com.tms.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private NotificationType type;
    private Boolean read;
    private String link;
    private LocalDateTime createdAt;
}

