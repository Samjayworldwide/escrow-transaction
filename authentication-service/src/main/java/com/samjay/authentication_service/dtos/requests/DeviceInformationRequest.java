package com.samjay.authentication_service.dtos.requests;

import com.samjay.authentication_service.enumerations.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceInformationRequest {

    @NotBlank(message = "Device IMEI is required")
    private String deviceImei;

    @NotBlank(message = "Firebase token is required")
    private String firebaseToken;

    private String deviceModel;

    private String osVersion;

    @NotNull(message = "Device platform is required")
    private DevicePlatform devicePlatform;
}
