package com.samjay.wallet_service.dtos.events;

public record DriverSearchEventDto(double sellerLatitude,
                                   double sellerLongitude,
                                   double deliveryFee,
                                   String pickupAddress,
                                   String dropOffAddress,
                                   String orderReferenceNumber,
                                   String clientRequestKey) {
}
