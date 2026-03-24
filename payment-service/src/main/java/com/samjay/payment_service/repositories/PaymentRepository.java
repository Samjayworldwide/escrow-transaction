package com.samjay.payment_service.repositories;

import com.samjay.payment_service.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("ALL")
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO payment (
                id,
                amount,
                order_id,
                user_id,
                payment_reference,
                description,
                transaction_status,
                version,
                created_at
            )
            VALUES (
                :id,
                :amount,
                :orderId,
                :userId,
                :paymentReference,
                :description,
                :transactionStatus,
                :version,
                now()
            )
            ON CONFLICT (payment_reference) DO NOTHING
            """, nativeQuery = true)
    int insertPaymentRecord(
            UUID id,
            BigDecimal amount,
            UUID orderId,
            UUID userId,
            String paymentReference,
            String description,
            String transactionStatus,
            Long version
    );

    Optional<Payment> findByPaymentReference(String paymentReference);

}
