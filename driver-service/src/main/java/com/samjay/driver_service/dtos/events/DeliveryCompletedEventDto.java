package com.samjay.driver_service.dtos.events;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryCompletedEventDto(UUID driverUserId,
                                        UUID buyerUserId,
                                        String driverEmail,
                                        BigDecimal deliveryFee,
                                        String orderReferenceNumber,
                                        UUID orderId,
                                        String clientRequestKey) {
}
