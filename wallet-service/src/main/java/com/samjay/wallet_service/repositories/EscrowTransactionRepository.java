package com.samjay.wallet_service.repositories;

import com.samjay.wallet_service.entities.EscrowTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, UUID> {

    boolean existsByOrderId(UUID orderId);
}
