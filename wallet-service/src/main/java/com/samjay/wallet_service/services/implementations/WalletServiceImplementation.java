package com.samjay.wallet_service.services.implementations;

import com.samjay.wallet_service.configurations.AuthenticatedUserProvider;
import com.samjay.wallet_service.dtos.events.DeliveryCompletedEventDto;
import com.samjay.wallet_service.dtos.events.DriverSearchEventDto;
import com.samjay.wallet_service.dtos.events.DriverWalletCreditNotificationEventDto;
import com.samjay.wallet_service.dtos.events.PaymentCompletionEventDto;
import com.samjay.wallet_service.dtos.responses.ApiResponse;
import com.samjay.wallet_service.dtos.responses.BuyerAndSellerBalanceResponse;
import com.samjay.wallet_service.dtos.responses.UserIdentifier;
import com.samjay.wallet_service.entities.Wallet;
import com.samjay.wallet_service.enumerations.LedgerEntryType;
import com.samjay.wallet_service.enumerations.ReferenceType;
import com.samjay.wallet_service.exceptions.ApplicationException;
import com.samjay.wallet_service.repositories.WalletRepository;
import com.samjay.wallet_service.services.interfaces.*;
import com.samjay.wallet_service.utility.AppExtensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.samjay.wallet_service.utility.AppExtensions.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImplementation implements WalletService {

    private final WalletRepository walletRepository;

    private final WalletLedgerService walletLedgerService;

    private final IdempotencyService idempotencyService;

    private final EscrowService escrowService;

    private final OutboxEventService outboxEventService;

    private final AuthenticatedUserProvider authenticatedUserProvider;

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

            String fingerprintKey = serialize(paymentCompletionEventDto);

            log.info("Generated fingerprint key for idempotency: {}", fingerprintKey);

            String requestFingerPrint = AppExtensions.generateHash(Objects.requireNonNull(fingerprintKey));

            boolean requestExists = idempotencyService.recordExists(
                    paymentCompletionEventDto.clientRequestKey(),
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

            BigDecimal buyerAvailableBalanceBeforeCredit = buyerWallet.getAvailableBalance();

            buyerWallet.setAvailableBalance(buyerAvailableBalanceBeforeCredit.add(paymentCompletionEventDto.amount()));

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

            escrowService.saveEscrowTransaction(
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
                    paymentCompletionEventDto.buyerUserId(),
                    paymentCompletionEventDto.sellerUserId(),
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

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public BuyerAndSellerBalanceResponse escrowRelease(UUID buyerWalletId, UUID sellerWalletId, BigDecimal amount, UUID escrowTransactionId) {

        Wallet buyerWallet = walletRepository.findById(buyerWalletId)
                .orElseThrow(() -> new ApplicationException(
                        "Buyer wallet not found for wallet ID: " + buyerWalletId,
                        HttpStatus.BAD_REQUEST)
                );

        Wallet sellerWallet = walletRepository.findById(sellerWalletId)
                .orElseThrow(() -> new ApplicationException(
                        "Seller wallet not found for wallet ID: " + sellerWalletId,
                        HttpStatus.BAD_REQUEST)
                );

        BigDecimal buyerLockedBalanceBeforeRelease = buyerWallet.getLockedBalance();

        buyerWallet.setLockedBalance(buyerLockedBalanceBeforeRelease.subtract(amount));

        walletLedgerService.saveLedgerEntry(
                buyerWallet,
                amount,
                LedgerEntryType.DEBIT,
                ReferenceType.ESCROW,
                escrowTransactionId
        );

        BigDecimal sellerAvailableBalanceBeforeCredit = sellerWallet.getAvailableBalance();

        sellerWallet.setAvailableBalance(sellerAvailableBalanceBeforeCredit.add(amount));

        walletLedgerService.saveLedgerEntry(
                sellerWallet,
                amount,
                LedgerEntryType.CREDIT,
                ReferenceType.ESCROW,
                escrowTransactionId
        );

        walletRepository.save(buyerWallet);

        walletRepository.save(sellerWallet);

        return new BuyerAndSellerBalanceResponse(buyerWallet.getAvailableBalance(), sellerWallet.getAvailableBalance());
    }

    @Transactional
    @Override
    public void creditDriver(DeliveryCompletedEventDto deliveryCompletedEventDto) {

        try {

            Wallet driverWallet = walletRepository
                    .findByUserId(deliveryCompletedEventDto.driverUserId())
                    .orElseThrow(() -> new ApplicationException(
                            "Wallet not found for driver with user ID: " + deliveryCompletedEventDto.driverUserId(),
                            HttpStatus.BAD_REQUEST)
                    );

            Wallet buyerWallet = walletRepository
                    .findByUserId(deliveryCompletedEventDto.buyerUserId())
                    .orElseThrow(() -> new ApplicationException(
                            "Wallet not found for buyer with user ID: " + deliveryCompletedEventDto.buyerUserId(),
                            HttpStatus.BAD_REQUEST)
                    );

            BigDecimal buyerLockedBalanceBeforeRelease = buyerWallet.getLockedBalance();

            if (buyerLockedBalanceBeforeRelease.compareTo(deliveryCompletedEventDto.deliveryFee()) < 0) {

                log.error("Buyer with user ID: {} has insufficient locked balance to release for delivery fee. Locked Balance: {}, Delivery Fee: {}",
                        deliveryCompletedEventDto.buyerUserId(), buyerLockedBalanceBeforeRelease, deliveryCompletedEventDto.deliveryFee());

                throw new ApplicationException(
                        "Buyer has insufficient locked balance to release for delivery fee.",
                        HttpStatus.BAD_REQUEST
                );
            }

            buyerWallet.setLockedBalance(buyerLockedBalanceBeforeRelease.subtract(deliveryCompletedEventDto.deliveryFee()));

            walletLedgerService.saveLedgerEntry(
                    buyerWallet,
                    deliveryCompletedEventDto.deliveryFee(),
                    LedgerEntryType.DEBIT,
                    ReferenceType.DELIVERY,
                    deliveryCompletedEventDto.orderId()
            );

            BigDecimal driverAvailableBalanceBeforeCredit = driverWallet.getAvailableBalance();

            driverWallet.setAvailableBalance(driverAvailableBalanceBeforeCredit.add(deliveryCompletedEventDto.deliveryFee()));

            walletLedgerService.saveLedgerEntry(
                    driverWallet,
                    deliveryCompletedEventDto.deliveryFee(),
                    LedgerEntryType.CREDIT,
                    ReferenceType.DELIVERY,
                    deliveryCompletedEventDto.orderId()
            );

            DriverWalletCreditNotificationEventDto driverWalletCreditNotificationEventDto = new DriverWalletCreditNotificationEventDto(
                    deliveryCompletedEventDto.driverUserId(),
                    deliveryCompletedEventDto.buyerUserId(),
                    deliveryCompletedEventDto.driverEmail(),
                    deliveryCompletedEventDto.orderReferenceNumber(),
                    deliveryCompletedEventDto.deliveryFee(),
                    driverWallet.getAvailableBalance(),
                    buyerWallet.getLockedBalance()
            );

            outboxEventService.saveEvent(
                    deliveryCompletedEventDto.driverUserId().toString(),
                    DRIVER_WALLET_CREDIT_NOTIFICATION_EVENT_TYPE,
                    DRIVER_WALLET_CREDIT_NOTIFICATION_KAFKA_BINDING,
                    driverWalletCreditNotificationEventDto,
                    deliveryCompletedEventDto.clientRequestKey()
            );

            walletRepository.save(driverWallet);

            walletRepository.save(buyerWallet);

        } catch (Exception ex) {

            log.error("Error crediting driver wallet for driver with user ID: {}", deliveryCompletedEventDto.driverUserId(), ex);

            throw ex;

        }
    }

    @McpTool(name = "Check-wallet-balance", description = "This tool allows you to check the user's current wallet available balance.")
    @Override
    public ApiResponse<BigDecimal> getWalletBalance() {

        log.info("Fetching wallet balance for the current user.");

        UserIdentifier userIdentifier = authenticatedUserProvider.getCurrentLoggedInUser();

        log.info("Current logged in user ID in wallet service: {}", userIdentifier.userId());

        Optional<Wallet> optionalWallet = walletRepository.findByUserId(UUID.fromString(userIdentifier.userId()));

        if (optionalWallet.isEmpty()) {

            log.warn("Wallet not found for user with user ID: {}", userIdentifier.userId());

            return ApiResponse.error("Wallet not found for the current user");
        }

        BigDecimal availableBalance = optionalWallet.get().getAvailableBalance();

        return ApiResponse.success("Balance fetched successfully.", availableBalance);

    }
}
