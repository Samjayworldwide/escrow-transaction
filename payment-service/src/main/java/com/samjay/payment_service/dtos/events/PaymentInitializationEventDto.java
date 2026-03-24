package com.samjay.payment_service.dtos.events;

public record PaymentInitializationEventDto(String email, String authorizationUrl, String orderReferenceNumber) {
}
