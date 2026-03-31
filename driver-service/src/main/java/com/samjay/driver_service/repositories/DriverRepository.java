package com.samjay.driver_service.repositories;

import com.samjay.driver_service.entities.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {

    boolean existsByUserId(UUID userId);

    @Query(
            """
                    SELECT d from Driver d
                    LEFT JOIN FETCH d.documents
                    WHERE d.userId = :driverId
                    """
    )
    Optional<Driver> findDriverByIdWithDocuments(@Param("driverId") UUID driverId);

    Optional<Driver> findByUserId(UUID userId);
}
