package com.samjay.wallet_service.services.interfaces;

import com.samjay.wallet_service.dtos.events.PaymentCompletionEventDto;

import java.util.UUID;

public interface WalletService {

    void createWallet(UUID userId);

    void creditWallet(PaymentCompletionEventDto paymentCompletionEventDto);
}
