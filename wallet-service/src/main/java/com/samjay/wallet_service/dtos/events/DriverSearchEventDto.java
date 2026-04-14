package com.samjay.wallet_service.dtos.events;

import java.util.UUID;

public record DriverSearchEventDto(double sellerLatitude,
                                   double sellerLongitude,
                                   double deliveryFee,
                                   UUID buyerUserId,
                                   UUID sellerUserId,
                                   String pickupAddress,
                                   String dropOffAddress,
                                   String orderReferenceNumber,
                                   String clientRequestKey) {
}
