package com.tms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class FileAttachmentResponse {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String storagePath;
    private LocalDateTime createdAt;
    private String createdBy;
}

