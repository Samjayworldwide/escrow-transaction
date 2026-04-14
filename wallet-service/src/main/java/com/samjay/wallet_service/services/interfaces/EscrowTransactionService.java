package com.samjay.wallet_service.services.interfaces;

import com.samjay.wallet_service.dtos.events.OrderSettlementEventDto;

import java.math.BigDecimal;
import java.util.UUID;

public interface EscrowTransactionService {

    void releaseEscrow(OrderSettlementEventDto orderSettlementEventDto);
}
