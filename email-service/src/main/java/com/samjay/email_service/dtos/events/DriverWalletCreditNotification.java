package com.samjay.email_service.dtos.events;

import java.math.BigDecimal;
import java.util.UUID;

public record DriverWalletCreditNotification(UUID driverUserId,
                                             String driverEmail,
                                             String orderReferenceNumber,
                                             BigDecimal amount,
                                             BigDecimal availableBalance) {
}
