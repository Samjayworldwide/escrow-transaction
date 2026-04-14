package com.samjay.order_service.dtos.events;

import java.util.UUID;

public record OrderDeliveryUpdateEventDto(UUID orderId, UUID driverUserId, String driverPhoneNumber) {
}
