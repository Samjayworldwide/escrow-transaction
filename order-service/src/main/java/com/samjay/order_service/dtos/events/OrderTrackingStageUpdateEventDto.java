package com.samjay.order_service.dtos.events;

import com.samjay.order_service.enumerations.TrackingStage;

import java.util.UUID;

public record OrderTrackingStageUpdateEventDto(UUID orderId, TrackingStage trackingStage) {
}
