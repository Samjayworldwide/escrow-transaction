package com.samjay.order_service.entities;

import com.samjay.order_service.enumerations.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uq_idempotency_key_event",
                columnNames = {"idempotencyKey", "eventType"}
        ),
        indexes = {
                @Index(name = "idx_idempotency_expires_at", columnList = "expiresAt"),
                @Index(name = "idx_idempotency_aggregate", columnList = "aggregateId, eventType")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 64)
    private String requestFingerprint;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @Column(nullable = false)
    private String responseMessage;

    @Column(nullable = false)
    private int responseStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime resolvedAt;
}
