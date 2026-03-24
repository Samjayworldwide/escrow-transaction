package com.samjay.order_service.repositories;

import com.samjay.order_service.entities.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

@SuppressWarnings("NullableProblems")
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKeyAndEventType(String idempotencyKey, String eventType);

    void deleteAllByExpiresAtBefore(LocalDateTime now);

    @Modifying
    @Query(value = """
            INSERT INTO idempotency_record (
                idempotency_key,
                aggregate_id,
                event_type,
                request_fingerprint,
                response_message,
                response_status,
                status,
                created_at,
                expires_at
            )
            VALUES (
                :idempotencyKey,
                :aggregateId,
                :eventType,
                :fingerPrint,
                'Request is being processed',
                0,
                'PROCESSING',
                :createdAt,
                :expiresAt
            )
            ON CONFLICT (idempotency_key, event_type) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(String idempotencyKey, String aggregateId, String eventType, String fingerPrint,
                             LocalDateTime createdAt, LocalDateTime expiresAt);
}
