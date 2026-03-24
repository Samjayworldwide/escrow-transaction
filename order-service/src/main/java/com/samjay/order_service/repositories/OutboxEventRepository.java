package com.samjay.order_service.repositories;

import com.samjay.order_service.entities.OutboxEvent;
import com.samjay.order_service.enumerations.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("ALL")
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO outbox_event (
                id,
                aggregate_id,
                event_type,
                payload,
                kafka_binding,
                idempotency_key,
                status,
                retry_count,
                max_retries,
                created_at,
                updated_at,
                version
            )
            VALUES (
                :id,
                :aggregateId,
                :eventType,
                :payload,
                :kafkaBinding,
                :idempotencyKey,
                :status,
                0,
                :maxRetries,
                now(),
                now(),
                0
            )
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(UUID id, String aggregateId, String eventType, String payload, String kafkaBinding,
                             String idempotencyKey, String status, int maxRetries);

    @Query("""
                SELECT o FROM OutboxEvent o
                WHERE o.status = :status
                ORDER BY o.createdAt ASC
                LIMIT :limit
            """)
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(@Param("status") OutboxEventStatus status, @Param("limit") int limit);

    @Query("""
                SELECT o FROM OutboxEvent o
                WHERE o.status = :status
                  AND o.retryCount < o.maxRetries
                ORDER BY o.updatedAt ASC
                LIMIT :limit
            """)
    List<OutboxEvent> findRetryableEvents(@Param("status") OutboxEventStatus status, @Param("limit") int limit);

    @Modifying
    @Query("""
                DELETE FROM OutboxEvent o
                WHERE o.status = :status
                  AND o.processedAt < :cutoff
            """)
    int deleteProcessedBefore(@Param("status") OutboxEventStatus status, @Param("cutoff") LocalDateTime cutoff);

    long countByStatus(OutboxEventStatus status);
}
