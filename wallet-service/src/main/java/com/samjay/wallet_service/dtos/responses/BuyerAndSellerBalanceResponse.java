package com.samjay.wallet_service.dtos.responses;

import java.math.BigDecimal;

public record BuyerAndSellerBalanceResponse(BigDecimal buyerAvailableBalance, BigDecimal sellerAvailableBalance) {
}
