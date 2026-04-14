package com.samjay.notification_service.dtos.events;

import java.util.UUID;

public record EmptyDriverSearchResultEventDto(UUID buyerUserId, UUID sellerUserId, String orderReferenceNumber) {
}
