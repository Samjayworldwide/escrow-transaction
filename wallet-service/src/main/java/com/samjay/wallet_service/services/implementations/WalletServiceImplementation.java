package com.samjay.wallet_service.services.implementations;

import com.samjay.wallet_service.dtos.events.DriverSearchEventDto;
import com.samjay.wallet_service.dtos.events.PaymentCompletionEventDto;
import com.samjay.wallet_service.entities.Wallet;
import com.samjay.wallet_service.enumerations.LedgerEntryType;
import com.samjay.wallet_service.enumerations.ReferenceType;
import com.samjay.wallet_service.exceptions.ApplicationException;
import com.samjay.wallet_service.repositories.WalletRepository;
import com.samjay.wallet_service.services.interfaces.*;
import com.samjay.wallet_service.utility.AppExtensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.samjay.wallet_service.utility.AppExtensions.DRIVER_SEARCH_EVENT_TYPE;
import static com.samjay.wallet_service.utility.AppExtensions.DRIVER_SEARCH_KAFKA_BINDING;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImplementation implements WalletService {

    private final WalletRepository walletRepository;

    private final WalletLedgerService walletLedgerService;

    private final IdempotencyService idempotencyService;

    private final EscrowTransactionService escrowTransactionService;

    private final OutboxEventService outboxEventService;

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
    public void creditWallet(PaymentCompletionEventDto paymentCompletionEventDto) {

        try {

            String fingerprintKey = paymentCompletionEventDto.clientRequestKey() + ":"
                    + paymentCompletionEventDto.buyerUserId() + ":" + paymentCompletionEventDto.sellerUserId() + ":"
                    + paymentCompletionEventDto.amount() + ":" + paymentCompletionEventDto.paymentId() + ":"
                    + paymentCompletionEventDto.orderId();

            log.info("Generated fingerprint key for idempotency: {}", fingerprintKey);

            String requestFingerPrint = AppExtensions.generateHash(fingerprintKey);

            boolean requestExists = idempotencyService.recordExists(paymentCompletionEventDto.clientRequestKey(),
                    AppExtensions.CREDIT_WALLET_EVENT_TYPE,
                    requestFingerPrint
            );

            if (requestExists) {

                log.warn("Duplicate request detected for client request key: {}", paymentCompletionEventDto.clientRequestKey());

                return;
            }

            Wallet buyerWallet = walletRepository
                    .findByUserId(paymentCompletionEventDto.buyerUserId())
                    .orElseThrow(() -> new ApplicationException(
                            "Wallet not found for user with user ID: " + paymentCompletionEventDto.buyerUserId(),
                            HttpStatus.BAD_REQUEST)
                    );

            Wallet sellerWallet = walletRepository
                    .findByUserId(paymentCompletionEventDto.sellerUserId())
                    .orElseThrow(() -> new ApplicationException(
                            "Wallet not found for user with user ID: " + paymentCompletionEventDto.sellerUserId(),
                            HttpStatus.BAD_REQUEST)
                    );

            BigDecimal buyerAvailableBalanceBeforeCredit = buyerWallet.getAvailableBalance();

            buyerWallet.setAvailableBalance(buyerAvailableBalanceBeforeCredit.add(paymentCompletionEventDto.amount()));

            int idemotencyRowsAffected = idempotencyService.createRecord(
                    paymentCompletionEventDto.clientRequestKey(),
                    paymentCompletionEventDto.buyerUserId().toString(),
                    AppExtensions.CREDIT_WALLET_EVENT_TYPE,
                    requestFingerPrint
            );

            if (idemotencyRowsAffected == 0) {

                log.warn("Duplicate request detected for client request key: {}", paymentCompletionEventDto.clientRequestKey());

                return;
            }

            walletLedgerService.saveLedgerEntry(
                    buyerWallet,
                    paymentCompletionEventDto.amount(),
                    LedgerEntryType.CREDIT,
                    ReferenceType.PAYMENT,
                    paymentCompletionEventDto.paymentId()
            );

            BigDecimal buyerAvailableBalanceAfterCredit = buyerWallet.getAvailableBalance();

            buyerWallet.setAvailableBalance(buyerAvailableBalanceAfterCredit.subtract(paymentCompletionEventDto.amount()));

            BigDecimal lockedBalance = buyerWallet.getLockedBalance();

            buyerWallet.setLockedBalance(lockedBalance.add(paymentCompletionEventDto.amount()));

            walletLedgerService.saveLedgerEntry(
                    buyerWallet,
                    paymentCompletionEventDto.amount(),
                    LedgerEntryType.DEBIT,
                    ReferenceType.ESCROW,
                    paymentCompletionEventDto.orderId()
            );

            escrowTransactionService.saveEscrowTransaction(
                    buyerWallet.getId(),
                    sellerWallet.getId(),
                    paymentCompletionEventDto.orderId(),
                    paymentCompletionEventDto.amount()
            );

            walletRepository.save(buyerWallet);

            DriverSearchEventDto driverSearchEventDto = new DriverSearchEventDto(
                    paymentCompletionEventDto.sellerLatitude(),
                    paymentCompletionEventDto.sellerLongitude(),
                    paymentCompletionEventDto.deliveryFee(),
                    paymentCompletionEventDto.pickupAddress(),
                    paymentCompletionEventDto.dropOffAddress(),
                    paymentCompletionEventDto.orderReferenceNumber(),
                    paymentCompletionEventDto.clientRequestKey()
            );

            outboxEventService.saveEvent(
                    paymentCompletionEventDto.buyerUserId().toString(),
                    DRIVER_SEARCH_EVENT_TYPE,
                    DRIVER_SEARCH_KAFKA_BINDING,
                    driverSearchEventDto,
                    paymentCompletionEventDto.clientRequestKey()
            );

        } catch (Exception ex) {

            log.error("Error credit wallet for user with user ID: {}", paymentCompletionEventDto.buyerUserId(), ex);

            throw ex;

        }
    }
}
