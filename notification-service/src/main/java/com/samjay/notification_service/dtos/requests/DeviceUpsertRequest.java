package com.samjay.notification_service.dtos.requests;


import com.samjay.notification_service.enumerations.DevicePlatform;

import java.util.UUID;


public record DeviceUpsertRequest(UUID userId,
                                  String deviceImei,
                                  String firebaseToken,
                                  String deviceModel,
                                  String osVersion,
                                  DevicePlatform devicePlatform) {
}
