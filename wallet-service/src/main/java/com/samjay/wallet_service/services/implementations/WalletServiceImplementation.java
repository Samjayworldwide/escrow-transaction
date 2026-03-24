package com.samjay.wallet_service.services.implementations;

import com.samjay.wallet_service.entities.Wallet;
import com.samjay.wallet_service.enumerations.LedgerEntryType;
import com.samjay.wallet_service.enumerations.ReferenceType;
import com.samjay.wallet_service.exceptions.ApplicationException;
import com.samjay.wallet_service.repositories.WalletRepository;
import com.samjay.wallet_service.services.interfaces.EscrowTransactionService;
import com.samjay.wallet_service.services.interfaces.IdempotencyService;
import com.samjay.wallet_service.services.interfaces.WalletLedgerService;
import com.samjay.wallet_service.services.interfaces.WalletService;
import com.samjay.wallet_service.utility.AppExtensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImplementation implements WalletService {

    private final WalletRepository walletRepository;

    private final WalletLedgerService walletLedgerService;

    private final IdempotencyService idempotencyService;

    private final EscrowTransactionService escrowTransactionService;

    @Transactional
    @Override
    public void createWallet(UUID userId) {

        try {

            if (userId == null) {

                log.warn("User ID is null. Cannot create wallet.");

                return;
            }

            boolean walletExists = walletRepository.existsByUserId(userId);

            if (walletExists) {

                log.warn("Wallet already exists for user with userId: {}", userId);

                return;
            }

            log.info("Creating wallet for user {}", userId);

            Wallet wallet = Wallet
                    .builder()
                    .userId(userId)
                    .build();

            walletRepository.save(wallet);

            log.info("Wallet created successfully for user {}", userId);

        } catch (Exception e) {

            log.error("Error creating wallet for user with userId: {}", userId, e);

            throw e;
        }
    }

    @Transactional
    @Override
    public void creditWallet(String clientRequestKey, UUID buyerUserId, UUID sellerUserId, BigDecimal amount, UUID paymentId, UUID orderId) {

        try {

            String fingerprintKey = clientRequestKey + ":" + buyerUserId + ":" + sellerUserId + ":" + amount + ":" + paymentId + ":" + orderId;

            log.info("Generated fingerprint key for idempotency: {}", fingerprintKey);

            String requestFingerPrint = AppExtensions.generateHash(fingerprintKey);

            boolean requestExists = idempotencyService.recordExists(clientRequestKey,
                    AppExtensions.CREDIT_WALLET_EVENT_TYPE,
                    requestFingerPrint
            );

            if (requestExists) {

                log.warn("Duplicate request detected for client request key: {}", clientRequestKey);

                return;
            }

            Wallet buyerWallet = walletRepository
                    .findByUserId(buyerUserId)
                    .orElseThrow(() -> new ApplicationException(
                            "Wallet not found for user with user ID: " + buyerUserId,
                            HttpStatus.BAD_REQUEST)
                    );

            Wallet sellerWallet = walletRepository
                    .findByUserId(sellerUserId)
                    .orElseThrow(() -> new ApplicationException(
                            "Wallet not found for user with user ID: " + sellerUserId,
                            HttpStatus.BAD_REQUEST)
                    );

            BigDecimal buyerAvailableBalanceBeforeCredit = buyerWallet.getAvailableBalance();

            buyerWallet.setAvailableBalance(buyerAvailableBalanceBeforeCredit.add(amount));

            int idemotencyRowsAffected = idempotencyService.createRecord(
                    clientRequestKey,
                    buyerUserId.toString(),
                    AppExtensions.CREDIT_WALLET_EVENT_TYPE,
                    requestFingerPrint
            );

            if (idemotencyRowsAffected == 0) {

                log.warn("Duplicate request detected for client request key: {}", clientRequestKey);

                return;
            }

            walletLedgerService.saveLedgerEntry(
                    buyerWallet,
                    amount,
                    LedgerEntryType.CREDIT,
                    ReferenceType.PAYMENT,
                    paymentId);

            BigDecimal buyerAvailableBalanceAfterCredit = buyerWallet.getAvailableBalance();

            buyerWallet.setAvailableBalance(buyerAvailableBalanceAfterCredit.subtract(amount));

            BigDecimal lockedBalance = buyerWallet.getLockedBalance();

            buyerWallet.setLockedBalance(lockedBalance.add(amount));

            walletLedgerService.saveLedgerEntry(
                    buyerWallet,
                    amount,
                    LedgerEntryType.DEBIT,
                    ReferenceType.ESCROW,
                    orderId
            );

            escrowTransactionService.saveEscrowTransaction(
                    buyerWallet.getId(),
                    sellerWallet.getId(),
                    orderId,
                    amount
            );

            walletRepository.save(buyerWallet);

        } catch (Exception ex) {

            log.error("Error credit wallet for user with user ID: {}", buyerUserId, ex);

            throw ex;

        }
    }
}
