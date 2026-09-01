package com.tms.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "webhook_registrations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class WebhookRegistration extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 500)
    private String eventTypes; // comma-separated: TRIP_STATUS_CHANGED,BOOKING_CREATED

    @Column(length = 100)
    private String secret; // for HMAC signature

    @Builder.Default
    private boolean active = true;

    @Column(length = 200)
    private String description;
}

