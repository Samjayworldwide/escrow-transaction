package com.samjay.driver_service.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmUploadRequest {

    @NotBlank(message = "Driver license blob name is required.")
    private String driverLicenseBlobName;

    @NotBlank(message = "Vehicle registration blob name is required.")
    private String vehicleRegistrationBlobName;

    @NotBlank(message = "National ID blob name is required.")
    private String nationalIdBlobName;
}
