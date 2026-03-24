package com.samjay.payment_service.entities;

import com.samjay.payment_service.enumerations.OutboxEventStatus;
import com.samjay.payment_service.utility.AppExtensions;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        indexes = {
                @Index(name = "idx_outbox_status_created", columnList = "status, createdAt"),
                @Index(name = "idx_outbox_aggregate_id", columnList = "aggregateId"),
                @Index(name = "idx_outbox_idempotency_key", columnList = "idempotencyKey")
        },
        uniqueConstraints = {

                @UniqueConstraint(name = "uq_outbox_idempotency_key", columnNames = "idempotencyKey")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 64)
    private String idempotencyKey;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 200)
    private String kafkaBinding;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int maxRetries = 5;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    private LocalDateTime processedAt;

    @PreUpdate
    protected void onUpdate() {

        this.updatedAt = LocalDateTime.now();
    }

    public void incrementRetry(String errorMessage) {

        this.retryCount++;

        this.lastError = errorMessage;

        if (this.retryCount >= this.maxRetries) {

            this.status = OutboxEventStatus.DEAD_LETTER;

        } else {

            this.status = OutboxEventStatus.FAILED;
        }
    }

    public void markProcessed() {

        this.status = OutboxEventStatus.PROCESSED;

        this.processedAt = LocalDateTime.now();
    }

    public static String generateIdempotencyKey(String aggregateId, String eventType, String clientRequestKey, String payload) {

        String seed = aggregateId + "::" + eventType + "::" + clientRequestKey + "::" + payload;

        return AppExtensions.generateHash(seed);
    }
}
