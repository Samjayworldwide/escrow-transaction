package com.samjay.driver_service.services.implementations;

import com.samjay.driver_service.configurations.AuthenticatedUserProvider;
import com.samjay.driver_service.configurations.DriverSessionRegistry;
import com.samjay.driver_service.dtos.events.DriverSearchEventDto;
import com.samjay.driver_service.dtos.events.UserRegisteredEventDto;
import com.samjay.driver_service.dtos.requests.CompleteProfileRequest;
import com.samjay.driver_service.dtos.responses.ApiResponse;
import com.samjay.driver_service.dtos.responses.SearchDriverH3Response;
import com.samjay.driver_service.dtos.responses.UserIdentifier;
import com.samjay.driver_service.entities.Driver;
import com.samjay.driver_service.exceptions.ApplicationException;
import com.samjay.driver_service.models.DriverLocation;
import com.samjay.driver_service.repositories.DriverRepository;
import com.samjay.driver_service.services.interfaces.DriverService;
import com.samjay.driver_service.services.interfaces.H3DriverMatchingService;
import com.samjay.driver_service.services.interfaces.MediaUploadService;
import com.samjay.driver_service.utility.AppExtensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverServiceImplementation implements DriverService {

    private final DriverRepository driverRepository;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    private final MediaUploadService mediaUploadService;

    private final H3DriverMatchingService h3DriverMatchingService;

    private final DriverSessionRegistry driverSessionRegistry;

    @Override
    @Transactional
    public void createDriver(UserRegisteredEventDto userRegisteredEventDto) {

        try {

            if (userRegisteredEventDto == null || userRegisteredEventDto.userId() == null) {

                log.warn("Received null UserRegisteredRecordDto. Skipping driver creation.");

                return;
            }

            boolean driverExists = driverRepository.existsByUserId(userRegisteredEventDto.userId());

            if (driverExists) {

                log.info("Driver with userId {} already exists. Skipping creation.", userRegisteredEventDto.userId());

                return;
            }

            Driver driver = Driver
                    .builder()
                    .email(userRegisteredEventDto.email())
                    .firstname(userRegisteredEventDto.firstname())
                    .lastname(userRegisteredEventDto.lastname())
                    .userId(userRegisteredEventDto.userId())
                    .build();

            driverRepository.save(driver);

            log.info("Driver with userId {} created successfully.", userRegisteredEventDto.userId());

        } catch (Exception e) {

            log.info("An error occurred while creating driver with userId {}: {}", userRegisteredEventDto != null
                    ? userRegisteredEventDto.userId() : null, e.getMessage(), e);

            throw e;

        }
    }

    @Transactional
    @Override
    public ApiResponse<String> completeProfile(CompleteProfileRequest completeProfileRequest) {

        UserIdentifier currentUser = authenticatedUserProvider.getCurrentLoggedInUser();

        Optional<Driver> driverOptional = driverRepository.findByUserId(UUID.fromString(currentUser.userId()));

        if (driverOptional.isEmpty())
            return ApiResponse.error("Driver profile not found");

        String url = mediaUploadService.upload(completeProfileRequest.getProfilePicture());

        Driver driver = driverOptional.get();

        try {

            driver.setProfilePictureUrl(url);

            driver.setPhoneNumber(completeProfileRequest.getPhoneNumber());

            driver.setLicensePlateNumber(completeProfileRequest.getLicensePlateNumber());

            driver.setIdentificationNumber(completeProfileRequest.getIdentificationNumber());

            double profileCompletion = AppExtensions.calculateCompletion(driver);

            driver.setProfileCompletion(profileCompletion);

            driverRepository.save(driver);

            return ApiResponse.success("Driver profile completed successfully");

        } catch (Exception ex) {

            log.error("An error occurred while completing driver profile: {}", ex.getMessage(), ex);

            try {

                mediaUploadService.delete(url);

            } catch (Exception deleteEx) {

                log.error("Failed to delete uploaded media after profile completion failure: {}", deleteEx.getMessage(), deleteEx);
            }

            throw new ApplicationException("Failed to complete driver profile. Please try again.", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public void searchForDriverClosestToSeller(DriverSearchEventDto driverSearchEventDto) {

        try {

            SearchDriverH3Response searchDriverH3Response = h3DriverMatchingService.findNearestDrivers(
                    driverSearchEventDto.sellerLatitude(),
                    driverSearchEventDto.sellerLongitude()
            );

            String messageBody = "Order delivery notification for order with reference number: "
                    + driverSearchEventDto.orderReferenceNumber() +
                    "Pickup address: -> " + driverSearchEventDto.pickupAddress() +
                    "drop-off address: -> " + driverSearchEventDto.dropOffAddress() +
                    "Delivery fee: -> ₦" + driverSearchEventDto.deliveryFee();

            List<DriverLocation> driverLocations = searchDriverH3Response.getDrivers();

            if (searchDriverH3Response.getTotalDriversFound() == 0 || driverLocations.isEmpty()) {

                log.info("There are currently no drivers found");

                //TODO Send push notification to seller and buyer informing them that no buyer was found
            }

            List<UUID> driversUserId = driverLocations.stream().map(DriverLocation::getUserId).toList();

            for (UUID userId : driversUserId) {

                WebSocketSession webSocketSession = driverSessionRegistry.getSession(userId);

                if (webSocketSession.isOpen()) {

                    webSocketSession.sendMessage(new TextMessage(messageBody));
                }

                //TODO Send push notification to drivers
            }

        } catch (Exception ex) {

            log.error("An error occurred while finding nearest driver: {}", ex.getMessage(), ex);

            throw new ApplicationException("Failed to find nearest driver. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
