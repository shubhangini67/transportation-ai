package com.tms.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "file_attachments", indexes = {
        @Index(name = "idx_attachment_entity", columnList = "entityType,entityId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class FileAttachment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String entityType; // TRIP, VEHICLE, LR

    @Column(nullable = false)
    private UUID entityId;

    @Column(nullable = false, length = 300)
    private String fileName;

    @Column(nullable = false, length = 100)
    private String fileType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 500)
    private String storagePath;

    @Column(name = "tenant_id")
    private UUID tenantId;
}

