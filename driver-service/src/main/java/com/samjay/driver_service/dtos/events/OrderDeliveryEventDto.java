package com.samjay.driver_service.dtos.events;

import java.util.UUID;

public record OrderDeliveryEventDto(UUID driverUserId, String notificationTitle, String notificationBody) {
}
