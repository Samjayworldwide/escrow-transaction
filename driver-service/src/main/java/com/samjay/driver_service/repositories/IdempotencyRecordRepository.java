package com.samjay.driver_service.repositories;

import com.samjay.driver_service.entities.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@SuppressWarnings("NullableProblems")
@Repository
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
                created_at,
                expires_at
            )
            VALUES (
                :idempotencyKey,
                :aggregateId,
                :eventType,
                :fingerPrint,
                'Request is being processed',
                :createdAt,
                :expiresAt
            )
            ON CONFLICT (idempotency_key, event_type) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(@Param("idempotencyKey") String idempotencyKey,
                             @Param("aggregateId") String aggregateId,
                             @Param("eventType") String eventType,
                             @Param("fingerPrint") String fingerPrint,
                             @Param("createdAt") LocalDateTime createdAt,
                             @Param("expiresAt") LocalDateTime expiresAt
    );
}
