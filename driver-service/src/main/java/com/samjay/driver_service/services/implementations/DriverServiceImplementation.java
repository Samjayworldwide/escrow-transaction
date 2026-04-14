package com.samjay.driver_service.services.implementations;

import com.samjay.OrderDetailsForDriverSearchResponse;
import com.samjay.OrderDetailsResponse;
import com.samjay.driver_service.configurations.AuthenticatedUserProvider;
import com.samjay.driver_service.configurations.DriverSessionRegistry;
import com.samjay.driver_service.dtos.events.*;
import com.samjay.driver_service.dtos.requests.CompleteProfileRequest;
import com.samjay.driver_service.dtos.responses.*;
import com.samjay.driver_service.entities.Driver;
import com.samjay.driver_service.enumerations.DriverStatus;
import com.samjay.driver_service.exceptions.ApplicationException;
import com.samjay.driver_service.models.DriverLocation;
import com.samjay.driver_service.repositories.DriverRepository;
import com.samjay.driver_service.services.grpcservice.OrderService;
import com.samjay.driver_service.services.interfaces.*;
import com.samjay.driver_service.utility.AppExtensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.samjay.driver_service.utility.AppExtensions.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverServiceImplementation implements DriverService {

    private final DriverRepository driverRepository;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    private final MediaUploadService mediaUploadService;

    private final H3DriverMatchingService h3DriverMatchingService;

    private final DriverSessionRegistry driverSessionRegistry;

    private final OutboxEventService outboxEventService;

    private final OrderService orderService;

    private final IdempotencyService idempotencyService;

    private final DriverTaskService driverTaskService;

    @Transactional
    @Override
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

    @Transactional
    @Override
    public void searchForDriverClosestToSeller(DriverSearchEventDto driverSearchEventDto) {

        try {

            log.info("THIS IS THE SELLERS LOCATION: Latitude: {}, Longitude: {}", driverSearchEventDto.sellerLatitude(), driverSearchEventDto.sellerLongitude());

            SearchDriverH3Response searchDriverH3Response = h3DriverMatchingService.findNearestDrivers(
                    driverSearchEventDto.sellerLatitude(),
                    driverSearchEventDto.sellerLongitude()
            );

            List<DriverLocation> driverLocations = searchDriverH3Response.getDrivers();

            if (searchDriverH3Response.getTotalDriversFound() == 0 || driverLocations.isEmpty()) {

                log.info("There are currently no drivers found");

                EmptyDriverSearchResultEventDto emptyDriverSearchResultEventDto = new EmptyDriverSearchResultEventDto(
                        driverSearchEventDto.buyerUserId(),
                        driverSearchEventDto.sellerUserId(),
                        driverSearchEventDto.orderReferenceNumber()
                );

                outboxEventService.saveEvent(
                        driverSearchEventDto.buyerUserId().toString() + "-" + driverSearchEventDto.sellerUserId().toString(),
                        EMPTY_DRIVER_SEARCH_RESULT_EVENT_TYPE,
                        EMPTY_DRIVER_SEARCH_RESULT_KAFKA_BINDING,
                        emptyDriverSearchResultEventDto,
                        driverSearchEventDto.clientRequestKey()
                );

                return;
            }

            String messageBody = "Order delivery notification for order with reference number: "
                    + driverSearchEventDto.orderReferenceNumber() +
                    "Pickup address: -> " + driverSearchEventDto.pickupAddress() +
                    "drop-off address: -> " + driverSearchEventDto.dropOffAddress() +
                    "Delivery fee: -> ₦" + driverSearchEventDto.deliveryFee();

            List<UUID> driversUserId = driverLocations.stream().map(DriverLocation::getUserId).toList();

            List<UUID> eligibleDriversUserId = checkIfDriverIsEligible(driversUserId);

            for (UUID userId : eligibleDriversUserId) {

                OrderDeliveryEventDto orderDeliveryEventDto = new OrderDeliveryEventDto(
                        userId,
                        "ORDER DELIVERY NOTIFICATION",
                        messageBody
                );

                outboxEventService.saveEvent(
                        userId.toString(),
                        ORDER_DELIVERY_NOTIFICATION_EVENT_TYPE,
                        ORDER_DELIVERY_NOTIFICATION_KAFKA_BINDING,
                        orderDeliveryEventDto,
                        driverSearchEventDto.clientRequestKey()
                );

                WebSocketSession webSocketSession = driverSessionRegistry.getSession(userId);

                if (webSocketSession.isOpen()) {

                    webSocketSession.sendMessage(new TextMessage(messageBody));

                }
            }

        } catch (Exception ex) {

            log.error("An error occurred while finding nearest driver: {}", ex.getMessage(), ex);

            throw new ApplicationException("Failed to find nearest driver. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    @Transactional
    @Override
    public ApiResponse<String> acceptDeliveryRequest(String clientRequestKey, UUID orderId) {

        UserIdentifier userIdentifier = authenticatedUserProvider.getCurrentLoggedInUser();

        String incomingFingerprint = AppExtensions.generateHash(orderId.toString() + userIdentifier.userId());

        Optional<ApiResponse<String>> idempotencyCheck = idempotencyService.checkKeyExists(
                clientRequestKey,
                ORDER_DRIVER_ASSIGNMENT_EVENT_TYPE,
                incomingFingerprint,
                String.class
        );

        if (idempotencyCheck.isPresent())
            return idempotencyCheck.get();

        Optional<Driver> optionalDriver = driverRepository.findByUserId(UUID.fromString(userIdentifier.userId()));

        if (optionalDriver.isEmpty())
            return ApiResponse.error("Driver profile not found");

        Driver driver = optionalDriver.get();

        boolean isOrderAccepted = driverTaskService.isOrderAccepted(orderId);

        if (isOrderAccepted)
            return ApiResponse.error("This delivery request has already been accepted");

        ApiResponse<OrderDetailsResponse> apiResponse = orderService.getOrderDetails(orderId);

        if (!apiResponse.isSuccessful())
            return ApiResponse.error("Failed to fetch order details. Please try again.");

        OrderDetailsResponse orderDetailsResponse = apiResponse.getResponseBody();

        if (!orderDetailsResponse.getIsFound())
            return ApiResponse.error("Order not found for the given order ID.");

        int insertedRows = idempotencyService.saveKey(
                clientRequestKey,
                userIdentifier.userId(),
                ORDER_DRIVER_ASSIGNMENT_EVENT_TYPE,
                incomingFingerprint
        );

        if (insertedRows == 0)
            return ApiResponse.error("Duplicate request. This delivery acceptance is processing or has already been processed.");

        String pickupCode = AppExtensions.generateVerificationCode();

        String dropOffCode = AppExtensions.generateVerificationCode();

        int driverTaskInsertedRows = driverTaskService.createDriverTaskIgnoreConflict(
                orderId,
                orderDetailsResponse.getOrderReferenceNumber(),
                BigDecimal.valueOf(orderDetailsResponse.getDeliveryFee()),
                orderDetailsResponse.getPickupAddress(),
                orderDetailsResponse.getDropOffAddress(),
                pickupCode,
                dropOffCode,
                driver.getId()
        );

        if (driverTaskInsertedRows == 0) {

            idempotencyService.markKeyAsFailed(
                    clientRequestKey,
                    ORDER_DRIVER_ASSIGNMENT_EVENT_TYPE,
                    "Delivery request has already been accepted."
            );

            return ApiResponse.error("Delivery request has already been accepted.");
        }

        driver.setDriverStatus(DriverStatus.UNAVAILABLE);

        driverRepository.save(driver);

        OrderDeliveryUpdateEventDto orderDeliveryUpdateEventDto = new OrderDeliveryUpdateEventDto(
                orderId,
                driver.getUserId(),
                driver.getPhoneNumber()
        );

        outboxEventService.saveEvent(
                userIdentifier.userId(),
                ORDER_DELIVERY_UPDATE_EVENT_TYPE,
                ORDER_DELIVERY_UPDATE_KAFKA_BINDING,
                orderDeliveryUpdateEventDto,
                clientRequestKey
        );

        NotificationDeliveryAcceptanceEventDto notificationDeliveryAcceptanceEventDto = new NotificationDeliveryAcceptanceEventDto(
                driver.getFirstname(),
                driver.getLastname(),
                driver.getPhoneNumber(),
                driver.getLicensePlateNumber(),
                UUID.fromString(orderDetailsResponse.getBuyerUserId()),
                UUID.fromString(orderDetailsResponse.getSellerUserId()),
                orderDetailsResponse.getOrderReferenceNumber(),
                pickupCode,
                dropOffCode
        );

        outboxEventService.saveEvent(
                userIdentifier.userId(),
                ORDER_DRIVER_ASSIGNMENT_EVENT_TYPE,
                NOTIFICATION_DELIVERY_ACCEPTANCE_KAFKA_BINDING,
                notificationDeliveryAcceptanceEventDto,
                clientRequestKey
        );

        EmailDeliveryAcceptanceEventDto emailDeliveryAcceptanceEventDto = new EmailDeliveryAcceptanceEventDto(
                orderDetailsResponse.getBuyerEmail(),
                orderDetailsResponse.getSellerEmail(),
                orderDetailsResponse.getOrderReferenceNumber(),
                pickupCode,
                dropOffCode,
                driver.getFirstname(),
                driver.getLastname(),
                driver.getPhoneNumber(),
                driver.getLicensePlateNumber()
        );

        outboxEventService.saveEvent(
                userIdentifier.userId(),
                ORDER_DRIVER_ASSIGNMENT_EVENT_TYPE,
                EMAIL_DELIVERY_ACCEPTANCE_KAFKA_BINDING,
                emailDeliveryAcceptanceEventDto,
                clientRequestKey
        );

        idempotencyService.markKeyAsSuccess(
                clientRequestKey,
                ORDER_DRIVER_ASSIGNMENT_EVENT_TYPE,
                "Delivery request accepted successfully.",
                null
        );

        return ApiResponse.success("Delivery request accepted successfully.");
    }

    @Transactional
    @Override
    public ApiResponse<List<DriverLocation>> searchForNearbyDrivers(UUID orderId) {

        try {

            ApiResponse<OrderDetailsForDriverSearchResponse> apiResponse = orderService.getOrderDetailsForDriverSearch(orderId);

            if (!apiResponse.isSuccessful())
                return ApiResponse.error("Failed to fetch order details. Please try again.");

            OrderDetailsForDriverSearchResponse orderDetailsForDriverSearchResponse = apiResponse.getResponseBody();

            if (!orderDetailsForDriverSearchResponse.getIsFound())
                return ApiResponse.error("Order not found for the given order ID.");

            DriverSearchEventDto driverSearchEventDto = new DriverSearchEventDto(
                    orderDetailsForDriverSearchResponse.getPickupLatitude(),
                    orderDetailsForDriverSearchResponse.getPickupLongitude(),
                    orderDetailsForDriverSearchResponse.getDeliveryFee(),
                    UUID.fromString(orderDetailsForDriverSearchResponse.getBuyerUserId()),
                    UUID.fromString(orderDetailsForDriverSearchResponse.getSellerUserId()),
                    orderDetailsForDriverSearchResponse.getPickupAddress(),
                    orderDetailsForDriverSearchResponse.getDropOffAddress(),
                    orderDetailsForDriverSearchResponse.getOrderReferenceNumber(),
                    AppExtensions.generateHash(orderId.toString())
            );

            SearchDriverH3Response searchDriverH3Response = h3DriverMatchingService.findNearestDrivers(
                    driverSearchEventDto.sellerLatitude(),
                    driverSearchEventDto.sellerLongitude()
            );

            List<DriverLocation> driverLocations = searchDriverH3Response.getDrivers();

            if (searchDriverH3Response.getTotalDriversFound() == 0 || driverLocations.isEmpty()) {

                log.info("There are currently no drivers found");

                EmptyDriverSearchResultEventDto emptyDriverSearchResultEventDto = new EmptyDriverSearchResultEventDto(
                        driverSearchEventDto.buyerUserId(),
                        driverSearchEventDto.sellerUserId(),
                        driverSearchEventDto.orderReferenceNumber()
                );

                outboxEventService.saveEvent(
                        driverSearchEventDto.buyerUserId().toString() + "-" + driverSearchEventDto.sellerUserId().toString(),
                        EMPTY_DRIVER_SEARCH_RESULT_EVENT_TYPE,
                        EMPTY_DRIVER_SEARCH_RESULT_KAFKA_BINDING,
                        emptyDriverSearchResultEventDto,
                        driverSearchEventDto.clientRequestKey()
                );

                return ApiResponse.error("No nearby drivers found for this order at the moment. Please try again later.");
            }

            String messageBody = "Order delivery notification for order with reference number: "
                    + driverSearchEventDto.orderReferenceNumber() +
                    "Pickup address: -> " + driverSearchEventDto.pickupAddress() +
                    "drop-off address: -> " + driverSearchEventDto.dropOffAddress() +
                    "Delivery fee: -> ₦" + driverSearchEventDto.deliveryFee();

            List<UUID> driversUserId = driverLocations.stream().map(DriverLocation::getUserId).toList();

            List<UUID> eligibleDriversUserId = checkIfDriverIsEligible(driversUserId);

            for (UUID userId : eligibleDriversUserId) {

                OrderDeliveryEventDto orderDeliveryEventDto = new OrderDeliveryEventDto(
                        userId,
                        "ORDER DELIVERY NOTIFICATION",
                        messageBody
                );

                WebSocketSession webSocketSession = driverSessionRegistry.getSession(userId);

                if (webSocketSession.isOpen()) {

                    webSocketSession.sendMessage(new TextMessage(messageBody));

                }

                outboxEventService.saveEvent(
                        userId.toString(),
                        ORDER_DELIVERY_NOTIFICATION_EVENT_TYPE,
                        ORDER_DELIVERY_NOTIFICATION_KAFKA_BINDING,
                        orderDeliveryEventDto,
                        driverSearchEventDto.clientRequestKey()
                );

            }

            return ApiResponse.success("Nearby drivers found and notified successfully. Waiting for driver to accept delivery request.", driverLocations);


        } catch (Exception ex) {

            log.error("An error occurred while searching for nearby drivers for order ID {}: {}", orderId, ex.getMessage(), ex);

            return ApiResponse.error("Failed to search for nearby drivers. Please try again.");
        }
    }


    private List<UUID> checkIfDriverIsEligible(List<UUID> driversUserId) {

        List<UUID> eligibleDrivers = new ArrayList<>();

        for (UUID userId : driversUserId) {

            Optional<Driver> driverOptional = driverRepository.findByUserId(userId);

            if (driverOptional.isPresent()) {

                Driver driver = driverOptional.get();

                if (driver.getProfileCompletion() >= 100.0 &&
                        driver.isDocumentVerified() &&
                        driver.getDriverStatus() == DriverStatus.AVAILABLE) {

                    eligibleDrivers.add(userId);
                }
            }
        }

        return eligibleDrivers;
    }
}
