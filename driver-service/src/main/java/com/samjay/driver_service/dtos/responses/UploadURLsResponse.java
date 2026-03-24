package com.samjay.driver_service.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadURLsResponse {

    private String driverLicenseUrl;

    private String driverLicenseBlobName;

    private String vehicleRegistrationUrl;

    private String vehicleRegistrationBlobName;

    private String nationalIdUrl;

    private String nationalIdBlobName;
}
