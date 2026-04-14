package com.samjay.authentication_service.dtos.events;

import com.samjay.authentication_service.enumerations.DevicePlatform;

import java.util.UUID;

public record DeviceInformationEventDto(
        UUID userId,
        String deviceImei,
        String firebaseToken,
        String deviceModel,
        String osVersion,
        DevicePlatform devicePlatform) {
}
