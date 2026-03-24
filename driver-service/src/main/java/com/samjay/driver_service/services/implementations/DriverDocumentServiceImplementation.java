package com.samjay.driver_service.services.implementations;

import com.samjay.driver_service.configurations.AuthenticatedUserProvider;
import com.samjay.driver_service.dtos.requests.ConfirmUploadRequest;
import com.samjay.driver_service.dtos.responses.ApiResponse;
import com.samjay.driver_service.dtos.responses.UploadURLsResponse;
import com.samjay.driver_service.dtos.responses.UserIdentifier;
import com.samjay.driver_service.entities.Driver;
import com.samjay.driver_service.entities.DriverDocument;
import com.samjay.driver_service.enumerations.DocumentType;
import com.samjay.driver_service.repositories.DriverRepository;
import com.samjay.driver_service.services.interfaces.AzureBlobService;
import com.samjay.driver_service.services.interfaces.DriverDocumentService;
import com.samjay.driver_service.utility.AppExtensions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverDocumentServiceImplementation implements DriverDocumentService {

    private final AzureBlobService azureBlobService;

    private final DriverRepository driverRepository;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Override
    public ApiResponse<UploadURLsResponse> generateUploadURLs() {

        String driverLicenseBlobName = azureBlobService.buildBlobName("driver-license");

        String vehicleRegistrationBlobName = azureBlobService.buildBlobName("vehicle-registration");

        String nationalIdBlobName = azureBlobService.buildBlobName("national-id");

        String driverLicenseUrl = azureBlobService.generateSasUrl(driverLicenseBlobName);

        String vehicleRegistrationUrl = azureBlobService.generateSasUrl(vehicleRegistrationBlobName);

        String nationalIdUrl = azureBlobService.generateSasUrl(nationalIdBlobName);

        UploadURLsResponse uploadURLsResponse = UploadURLsResponse
                .builder()
                .driverLicenseBlobName(driverLicenseBlobName)
                .driverLicenseUrl(driverLicenseUrl)
                .vehicleRegistrationBlobName(vehicleRegistrationBlobName)
                .vehicleRegistrationUrl(vehicleRegistrationUrl)
                .nationalIdBlobName(nationalIdBlobName)
                .nationalIdUrl(nationalIdUrl)
                .build();

        return ApiResponse.success("Upload tokens generated successfully.", uploadURLsResponse);
    }

    @Transactional
    @Override
    public ApiResponse<String> confirmUpload(ConfirmUploadRequest confirmUploadRequest) {

        UserIdentifier userIdentifier = authenticatedUserProvider.getCurrentLoggedInUser();

        boolean driverLicenseBlobExists = azureBlobService.blobExists(confirmUploadRequest.getDriverLicenseBlobName());

        if (!driverLicenseBlobExists)
            return ApiResponse.error("An error occurred uploading the driver license. Please try again.");

        boolean vehicleRegistrationBlobExists = azureBlobService.blobExists(confirmUploadRequest.getVehicleRegistrationBlobName());

        if (!vehicleRegistrationBlobExists)
            return ApiResponse.error("An error occurred uploading the vehicle registration. Please try again.");

        boolean nationalIdBlobExists = azureBlobService.blobExists(confirmUploadRequest.getNationalIdBlobName());

        if (!nationalIdBlobExists)
            return ApiResponse.error("An error occurred uploading the national ID. Please try again.");

        Optional<Driver> optionalDriver = driverRepository.findDriverByIdWithDocuments(UUID.fromString(userIdentifier.userId()));

        if (optionalDriver.isEmpty())
            return ApiResponse.error("Driver not found.");

        Driver driver = optionalDriver.get();

        DriverDocument driverLicenseDocument = DriverDocument
                .builder()
                .documentType(DocumentType.DRIVERS_LICENSE)
                .blobName(confirmUploadRequest.getDriverLicenseBlobName())
                .build();

        DriverDocument vehicleRegistrationDocument = DriverDocument
                .builder()
                .documentType(DocumentType.VEHICLE_REGISTRATION)
                .blobName(confirmUploadRequest.getVehicleRegistrationBlobName())
                .build();

        DriverDocument nationalIdDocument = DriverDocument
                .builder()
                .documentType(DocumentType.NATIONAL_ID)
                .blobName(confirmUploadRequest.getNationalIdBlobName())
                .build();

        driver.addDocument(driverLicenseDocument);

        driver.addDocument(vehicleRegistrationDocument);

        driver.addDocument(nationalIdDocument);

        double profileCompletion = AppExtensions.calculateCompletion(driver);

        driver.setProfileCompletion(profileCompletion);

        driverRepository.save(driver);

        return ApiResponse.success("Documents uploaded successfully. They are now pending verification.");
    }
}
