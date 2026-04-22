package com.samjay.order_service.repositories;

import com.samjay.order_service.entities.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    boolean existsByOrderIdAndCreatorUserId(UUID orderId, UUID creatorUserId);

}
