package com.samjay.driver_service.dtos.events;

public record TrackingStageNotificationEventDto(String userId, String orderReferenceNumber, boolean isBuyer) {
}
