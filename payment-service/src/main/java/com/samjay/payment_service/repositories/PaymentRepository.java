package com.samjay.payment_service.repositories;

import com.samjay.payment_service.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
                delivery_fee,
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
                :deliveryFee,
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
            @Param("id") UUID id,
            @Param("amount") BigDecimal amount,
            @Param("deliveryFee") BigDecimal deliveryFee,
            @Param("orderId") UUID orderId,
            @Param("userId") UUID userId,
            @Param("paymentReference") String paymentReference,
            @Param("description") String description,
            @Param("transactionStatus") String transactionStatus,
            @Param("version") Long version
    );

    Optional<Payment> findByPaymentReference(String paymentReference);

}
