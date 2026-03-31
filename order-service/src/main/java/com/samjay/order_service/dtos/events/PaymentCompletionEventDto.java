package com.samjay.order_service.dtos.events;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCompletionEventDto(
        String buyerEmail,
        String sellerEmail,
        UUID buyerUserId,
        UUID sellerUserId,
        double sellerLatitude,
        double sellerLongitude,
        double deliveryFee,
        String pickupAddress,
        String dropOffAddress,
        String orderReferenceNumber,
        BigDecimal amount,
        UUID paymentId,
        UUID orderId,
        String clientRequestKey) {
}
