package com.samjay.wallet_service.services.implementations;

import com.samjay.wallet_service.dtos.events.EscrowReleaseCompletedEventDto;
import com.samjay.wallet_service.dtos.events.OrderSettlementEventDto;
import com.samjay.wallet_service.dtos.responses.BuyerAndSellerBalanceResponse;
import com.samjay.wallet_service.entities.EscrowTransaction;
import com.samjay.wallet_service.enumerations.EscrowStatus;
import com.samjay.wallet_service.exceptions.ApplicationException;
import com.samjay.wallet_service.repositories.EscrowTransactionRepository;
import com.samjay.wallet_service.services.interfaces.EscrowTransactionService;
import com.samjay.wallet_service.services.interfaces.IdempotencyService;
import com.samjay.wallet_service.services.interfaces.OutboxEventService;
import com.samjay.wallet_service.services.interfaces.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.samjay.wallet_service.utility.AppExtensions.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscrowTransactionServiceImplementation implements EscrowTransactionService {

    private final EscrowTransactionRepository escrowTransactionRepository;

    private final IdempotencyService idempotencyService;

    private final WalletService walletService;

    private final OutboxEventService outboxEventService;

    @Transactional
    @Override
    public void releaseEscrow(OrderSettlementEventDto orderSettlementEventDto) {

        String jsonPayload = serialize(orderSettlementEventDto);

        String requestFingerprint = generateHash(Objects.requireNonNull(jsonPayload));

        boolean recordExists = idempotencyService.recordExists(
                orderSettlementEventDto.clientRequestKey(),
                ESCROW_RELEASE_EVENT_TYPE,
                requestFingerprint
        );

        if (recordExists) {

            log.warn("Duplicate request detected for order ID: {}. Client request key: {}. Ignoring duplicate.",
                    orderSettlementEventDto.orderId(), orderSettlementEventDto.clientRequestKey());

            return;
        }

        EscrowTransaction escrowTransaction = escrowTransactionRepository.findByOrderId(orderSettlementEventDto.orderId())
                .orElseThrow(() -> new ApplicationException(
                        "Escrow transaction not found for order ID: " + orderSettlementEventDto.orderId(),
                        HttpStatus.BAD_REQUEST)
                );

        if (escrowTransaction.getStatus() == EscrowStatus.RELEASED) {

            log.warn("Escrow transaction for order ID: {} has already been released. Client request key: {}. Ignoring duplicate release.",
                    orderSettlementEventDto.orderId(), orderSettlementEventDto.clientRequestKey());

            return;

        }

        int rowsInserted = idempotencyService.createRecord(
                orderSettlementEventDto.clientRequestKey(),
                orderSettlementEventDto.buyerUserId().toString(),
                ESCROW_RELEASE_EVENT_TYPE,
                requestFingerprint
        );

        if (rowsInserted == 0) {

            log.warn("Another request with the same client request key is already being processed for order ID: {}. Client request key: {}. Ignoring duplicate.",
                    orderSettlementEventDto.orderId(), orderSettlementEventDto.clientRequestKey());

            return;

        }

        BuyerAndSellerBalanceResponse buyerAndSellerBalanceResponse = walletService.escrowRelease(
                escrowTransaction.getBuyerWalletId(),
                escrowTransaction.getSellerWalletId(),
                escrowTransaction.getAmount(),
                escrowTransaction.getId()
        );

        escrowTransaction.setStatus(EscrowStatus.RELEASED);

        escrowTransaction.setReleasedAt(LocalDateTime.now());

        escrowTransactionRepository.save(escrowTransaction);

        EscrowReleaseCompletedEventDto escrowReleaseCompletedEventDto = new EscrowReleaseCompletedEventDto(
                orderSettlementEventDto.buyerUserId(),
                orderSettlementEventDto.sellerUserId(),
                orderSettlementEventDto.buyerEmail(),
                orderSettlementEventDto.sellerEmail(),
                orderSettlementEventDto.orderReferenceNumber(),
                escrowTransaction.getAmount(),
                buyerAndSellerBalanceResponse.buyerAvailableBalance(),
                buyerAndSellerBalanceResponse.sellerAvailableBalance()
        );

        outboxEventService.saveEvent(
                orderSettlementEventDto.buyerUserId().toString(),
                ESCROW_RELEASE_COMPLETED_EVENT_TYPE,
                ESCROW_RELEASE_COMPLETED_KAFKA_BINDING,
                escrowReleaseCompletedEventDto,
                orderSettlementEventDto.clientRequestKey()
        );

    }
}
