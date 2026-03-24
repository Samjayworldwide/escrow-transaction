package com.samjay.wallet_service.repositories;

import com.samjay.wallet_service.entities.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;


@SuppressWarnings("NullableProblems")
@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKeyAndEventTypeAndRequestFingerprint(String idempotencyKey, String eventType, String requestFingerPrint);

    @Modifying
    @Query(
            value = """
                    INSERT INTO idempotency_record (
                    idempotency_key,
                    aggregate_id,
                    event_type,
                    request_fingerprint,
                    response_status,
                    created_at,
                    expires_at
                    )
                    VALUES (
                    :idempotencyKey,
                    :aggregateId,
                    :eventType,
                    :requestFingerprint,
                    0,
                    :createdAt,
                    :expiresAt
                    )
                    ON CONFLICT (idempotency_key, event_type) DO NOTHING
                    """
            , nativeQuery = true
    )
    int insertRecord(String idempotencyKey,
                     String aggregateId,
                     String eventType,
                     String requestFingerprint,
                     LocalDateTime createdAt,
                     LocalDateTime expiresAt
    );

    void deleteAllByExpiresAtBefore(LocalDateTime now);
}