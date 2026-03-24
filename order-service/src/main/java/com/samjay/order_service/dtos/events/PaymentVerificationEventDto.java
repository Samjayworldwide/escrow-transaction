package com.samjay.order_service.dtos.events;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentVerificationEventDto(UUID orderId, UUID paymentId, BigDecimal amount, String clientRequestKey) {
}
