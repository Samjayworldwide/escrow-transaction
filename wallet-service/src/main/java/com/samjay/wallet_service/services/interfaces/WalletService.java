package com.samjay.wallet_service.services.interfaces;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {

    void createWallet(UUID userId);

    void creditWallet(String clientRequestKey,
                      UUID buyerUserId,
                      UUID sellerUserId,
                      BigDecimal amount,
                      UUID paymentId,
                      UUID orderId
    );
}
