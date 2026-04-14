package com.samjay.email_service.dtos.events;

public record EmailDeliveryAcceptanceEventDto(String buyerEmail,
                                              String sellerEmail,
                                              String orderReferenceNumber,
                                              String pickupDeliveryCode,
                                              String dropoffDeliveryCode,
                                              String driverFirstname,
                                              String driverLastname,
                                              String driverPhoneNumber,
                                              String vehicleLicenseNumber) {
}
