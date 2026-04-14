package com.samjay.notification_service.dtos.events;

import java.util.UUID;

public record NotificationDeliveryAcceptanceEventDto(String driverFirstname,
                                                     String driverLastname,
                                                     String driverPhoneNumber,
                                                     String vehicleLicenseNumber,
                                                     UUID buyerUserId,
                                                     UUID sellerUserId,
                                                     String orderReferenceNumber,
                                                     String pickupDeliveryCode,
                                                     String dropoffDeliveryCode) {
}
