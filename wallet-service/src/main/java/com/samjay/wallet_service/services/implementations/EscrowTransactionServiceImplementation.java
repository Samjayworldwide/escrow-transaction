package com.samjay.wallet_service.services.implementations;

import com.samjay.wallet_service.entities.EscrowTransaction;
import com.samjay.wallet_service.repositories.EscrowTransactionRepository;
import com.samjay.wallet_service.services.interfaces.EscrowTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscrowTransactionServiceImplementation implements EscrowTransactionService {

    private final EscrowTransactionRepository escrowTransactionRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void saveEscrowTransaction(UUID buyerWalletId, UUID sellerWalletId, UUID orderId, BigDecimal amount) {

        try {

            log.info("Saving escrow transaction for order ID: {}, buyer wallet ID: {}, seller wallet ID: {}, amount: {}",
                    orderId, buyerWalletId, sellerWalletId, amount);

            boolean transactionExists = escrowTransactionRepository.existsByOrderId(orderId);

            if (transactionExists) {

                log.warn("Escrow transaction already exists for order ID: {}", orderId);

                return;
            }

            EscrowTransaction escrowTransaction = EscrowTransaction
                    .builder()
                    .orderId(orderId)
                    .buyerWalletId(buyerWalletId)
                    .sellerWalletId(sellerWalletId)
                    .amount(amount)
                    .build();

            escrowTransactionRepository.save(escrowTransaction);

            log.info("Escrow transaction saved successfully for order ID: {}", orderId);

        } catch (Exception e) {

            log.error("Error saving escrow transaction for order ID: {}, buyer wallet ID: {}, seller wallet ID: {}, amount: {}",
                    orderId, buyerWalletId, sellerWalletId, amount, e);

            throw e;
        }
    }
}
