package com.tms.entity;

import com.tms.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id,createdAt"),
        @Index(name = "idx_notification_unread", columnList = "user_id,read")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Notification extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "read", nullable = false)
    @Builder.Default
    private Boolean read = false;

    @Column(length = 300)
    private String link;

    @Column(name = "tenant_id")
    private UUID tenantId;
}

