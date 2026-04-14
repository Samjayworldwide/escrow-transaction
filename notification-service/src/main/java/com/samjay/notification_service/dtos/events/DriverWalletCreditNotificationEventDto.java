package com.samjay.notification_service.dtos.events;

import java.math.BigDecimal;
import java.util.UUID;

public record DriverWalletCreditNotificationEventDto(UUID driverUserId,
                                                     UUID buyerUserId,
                                                     String driverEmail,
                                                     String orderReferenceNumber,
                                                     BigDecimal amount,
                                                     BigDecimal driverAvailableBalance,
                                                     BigDecimal buyerLockedBalance) {
}
