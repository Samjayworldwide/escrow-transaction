package com.samjay.notification_service.dtos.events;

import java.math.BigDecimal;
import java.util.UUID;

public record EscrowReleaseCompletedEventDto(UUID buyerUserId,
                                             UUID sellerUserId,
                                             String buyerEmail,
                                             String sellerEmail,
                                             String orderReferenceNumber,
                                             BigDecimal amount,
                                             BigDecimal buyerAvailableBalance,
                                             BigDecimal sellerAvailableBalance) {
}
