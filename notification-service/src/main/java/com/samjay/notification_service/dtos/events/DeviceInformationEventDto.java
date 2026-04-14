package com.samjay.notification_service.dtos.events;


import com.samjay.notification_service.enumerations.DevicePlatform;

import java.util.UUID;

public record DeviceInformationEventDto(
        UUID userId,
        String deviceImei,
        String firebaseToken,
        String deviceModel,
        String osVersion,
        DevicePlatform devicePlatform) {
}
