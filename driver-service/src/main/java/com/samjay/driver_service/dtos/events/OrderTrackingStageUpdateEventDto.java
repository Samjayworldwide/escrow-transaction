package com.samjay.driver_service.dtos.events;

import com.samjay.driver_service.enumerations.TrackingStage;

import java.util.UUID;

public record OrderTrackingStageUpdateEventDto(UUID orderId, TrackingStage trackingStage) {
}
