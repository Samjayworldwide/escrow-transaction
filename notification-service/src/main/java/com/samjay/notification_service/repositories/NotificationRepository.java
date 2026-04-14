package com.samjay.notification_service.repositories;

import com.samjay.notification_service.entities.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    @Query(
            value = """
                    SELECT n FROM Notification n
                    WHERE n.user_id = :userId
                    ORDER BY n.created_at DESC, n.id DESC
                    """,
            nativeQuery = true
    )
    List<Notification> findNotificationsFirstPage(@Param("userId") UUID userId, Pageable pageable);

    @Query(
            value = """
                    SELECT n FROM Notification n
                    WHERE n.user_id = :userId
                    AND (n.created_at < :lastCreatedAt OR (n.created_at = :lastCreatedAt AND n.id < :id))
                    ORDER BY n.created_at DESC, n.id DESC
                    """,
            nativeQuery = true
    )
    List<Notification> findNotificationsAfterCursor(@Param("userId") UUID userId,
                                                    @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
                                                    @Param("id") Long id,
                                                    Pageable pageable
    );

}
