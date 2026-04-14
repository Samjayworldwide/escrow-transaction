package com.samjay.wallet_service.services.interfaces;

import java.math.BigDecimal;
import java.util.UUID;

public interface EscrowService {

    void saveEscrowTransaction(UUID buyerWalletId, UUID sellerWalletId, UUID orderId, BigDecimal amount);
}
