package com.samjay.notification_service.dtos.events;

public record TrackingStageNotificationEventDto(String userId, String orderReferenceNumber, boolean isBuyer) {
}
