package com.samjay.wallet_service.services.interfaces;

import com.samjay.wallet_service.dtos.events.DeliveryCompletedEventDto;
import com.samjay.wallet_service.dtos.events.PaymentCompletionEventDto;
import com.samjay.wallet_service.dtos.responses.ApiResponse;
import com.samjay.wallet_service.dtos.responses.BuyerAndSellerBalanceResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {

    void createWallet(UUID userId);

    void creditWallet(PaymentCompletionEventDto paymentCompletionEventDto);

    BuyerAndSellerBalanceResponse escrowRelease(UUID buyerWalletId, UUID sellerWalletId, BigDecimal amount, UUID escrowTransactionId);

    void creditDriver(DeliveryCompletedEventDto deliveryCompletedEventDto);

    ApiResponse<BigDecimal> getWalletBalance();
}
