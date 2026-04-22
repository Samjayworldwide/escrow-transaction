package com.samjay.driver_service.repositories;

import com.samjay.driver_service.entities.DriverTask;
import com.samjay.driver_service.enumerations.DriverTaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface DriverTaskRepository extends JpaRepository<DriverTask, UUID> {

    boolean existsByOrderIdAndDriverTaskStatus(UUID orderId, DriverTaskStatus driverTaskStatus);

    @Query("""
            SELECT dt FROM DriverTask dt
            JOIN FETCH dt.driver
            WHERE dt.orderReferenceNumber = :orderReferenceNumber
            """)
    Optional<DriverTask> findByOrderReferenceNumberWithDriver(String orderReferenceNumber);

    @Modifying
    @Transactional
    @Query(
            value = """
                    INSERT INTO driver_task (
                        id,
                        order_id,
                        order_reference_number,
                        delivery_fee,
                        pickup_address,
                        dropoff_address,
                        pickup_delivery_code,
                        dropoff_delivery_code,
                        is_completed,
                        driver_task_status,
                        driver_id,
                        created_at
                    )
                    VALUES (
                        :id,
                        :orderId,
                        :orderReferenceNumber,
                        :deliveryFee,
                        :pickupAddress,
                        :dropoffAddress,
                        :pickupCode,
                        :dropoffCode,
                        false,
                        'ACCEPTED',
                        :driverId,
                        now()
                    )
                    ON CONFLICT (order_id)
                    WHERE driver_task_status = 'ACCEPTED'
                    DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIgnoreIfExists(
            @Param("id") UUID id,
            @Param("orderId") UUID orderId,
            @Param("orderReferenceNumber") String orderReferenceNumber,
            @Param("deliveryFee") BigDecimal deliveryFee,
            @Param("pickupAddress") String pickupAddress,
            @Param("dropoffAddress") String dropoffAddress,
            @Param("pickupCode") String pickupCode,
            @Param("dropoffCode") String dropoffCode,
            @Param("driverId") UUID driverId
    );

    @Query("""
                SELECT t FROM DriverTask t
                WHERE t.driver.id = :driverId
                AND t.driverTaskStatus = :status
                ORDER BY t.createdAt DESC, t.id DESC
            """)
    List<DriverTask> findTasksFirstPage(
            @Param("driverId") UUID driverId,
            @Param("status") DriverTaskStatus status,
            Pageable pageable
    );

    @Query("""
                SELECT t FROM DriverTask t
                WHERE t.driver.id = :driverId
                AND t.driverTaskStatus = :status
                AND (t.createdAt < :lastCreatedAt OR (t.createdAt = :lastCreatedAt AND t.id < :lastId))
                ORDER BY t.createdAt DESC, t.id DESC
            """)
    List<DriverTask> findTasksAfterCursor(
            @Param("driverId") UUID driverId,
            @Param("status") DriverTaskStatus status,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") UUID lastId,
            Pageable pageable
    );

}
