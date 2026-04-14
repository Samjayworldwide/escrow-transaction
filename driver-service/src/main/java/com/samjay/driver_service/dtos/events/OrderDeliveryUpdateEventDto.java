package com.samjay.driver_service.dtos.events;

import java.util.UUID;

public record OrderDeliveryUpdateEventDto(UUID orderId, UUID driverUserId, String driverPhoneNumber) {
}
