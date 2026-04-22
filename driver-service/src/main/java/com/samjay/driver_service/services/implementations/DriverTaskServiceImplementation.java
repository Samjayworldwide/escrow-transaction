package com.samjay.driver_service.services.implementations;

import com.samjay.OrderTrackingStageResponse;
import com.samjay.driver_service.configurations.AuthenticatedUserProvider;
import com.samjay.driver_service.dtos.events.DeliveryCompletedEventDto;
import com.samjay.driver_service.dtos.events.OrderTrackingStageUpdateEventDto;
import com.samjay.driver_service.dtos.events.TrackingStageNotificationEventDto;
import com.samjay.driver_service.dtos.requests.DeliveryCodeVerificationRequest;
import com.samjay.driver_service.dtos.requests.FetchDriverTaskRequest;
import com.samjay.driver_service.dtos.responses.*;
import com.samjay.driver_service.entities.Driver;
import com.samjay.driver_service.entities.DriverTask;
import com.samjay.driver_service.enumerations.DriverStatus;
import com.samjay.driver_service.enumerations.DriverTaskStatus;
import com.samjay.driver_service.enumerations.TrackingStage;
import com.samjay.driver_service.models.CursorPayload;
import com.samjay.driver_service.repositories.DriverRepository;
import com.samjay.driver_service.repositories.DriverTaskRepository;
import com.samjay.driver_service.services.grpcservice.OrderService;
import com.samjay.driver_service.services.interfaces.DriverTaskService;
import com.samjay.driver_service.services.interfaces.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.samjay.driver_service.utility.AppExtensions.*;
import static com.samjay.driver_service.utility.AppExtensions.TRACKING_STAGE_NOTIFICATION_KAFKA_BINDING;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverTaskServiceImplementation implements DriverTaskService {

    private final DriverTaskRepository driverTaskRepository;

    private final DriverRepository driverRepository;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    private final OrderService orderService;

    private final PasswordEncoder passwordEncoder;

    private final OutboxEventService outboxEventService;


    @Override
    public int createDriverTaskIgnoreConflict(UUID orderId,
                                              String orderReferenceNumber,
                                              BigDecimal deliveryFee,
                                              String pickupAddress,
                                              String dropoffAddress,
                                              String pickupCode,
                                              String dropoffCode,
                                              UUID driverId) {

        String hashedPickupCode = passwordEncoder.encode(pickupCode);

        String hashedDropoffCode = passwordEncoder.encode(dropoffCode);

        return driverTaskRepository.insertIgnoreIfExists(
                UUID.randomUUID(),
                orderId,
                orderReferenceNumber,
                deliveryFee,
                pickupAddress,
                dropoffAddress,
                hashedPickupCode,
                hashedDropoffCode,
                driverId
        );

    }

    @Override
    public boolean isOrderAccepted(UUID orderId) {

        return driverTaskRepository.existsByOrderIdAndDriverTaskStatus(orderId, DriverTaskStatus.ACCEPTED);

    }

    @Transactional
    @Override
    public ApiResponse<String> verifyDeliveryCode(String clientRequestKey, DeliveryCodeVerificationRequest deliveryCodeVerificationRequest) {

        UserIdentifier userIdentifier = authenticatedUserProvider.getCurrentLoggedInUser();

        Optional<Driver> optionalDriver = driverRepository.findByUserId(UUID.fromString(userIdentifier.userId()));

        if (optionalDriver.isEmpty())
            return ApiResponse.error("Driver not found with the current user ID.");

        Driver driver = optionalDriver.get();

        OrderTrackingStageUpdateEventDto orderTrackingStageUpdateEventDto;

        TrackingStageNotificationEventDto trackingStageNotificationEventDto;

        Optional<DriverTask> optionalDriverTask = driver
                .getDriverTasks()
                .stream()
                .filter(f -> f.getOrderId().equals(deliveryCodeVerificationRequest.getOrderId()))
                .findFirst();

        if (optionalDriverTask.isEmpty())
            return ApiResponse.error("Driver task not found for the given order ID.");

        DriverTask driverTask = optionalDriverTask.get();

        boolean isDeliveryCompleted = driverTask.isCompleted();

        if (isDeliveryCompleted)
            return ApiResponse.error("Delivery code has already been verified for this order. The delivery is marked as completed.");

        ApiResponse<OrderTrackingStageResponse> apiResponse = orderService.getOrderTrackingStage(deliveryCodeVerificationRequest.getOrderId());

        if (!apiResponse.isSuccessful())
            return ApiResponse.error(apiResponse.getResponseMessage());

        OrderTrackingStageResponse orderTrackingStageResponse = apiResponse.getResponseBody();

        if (!orderTrackingStageResponse.getIsFound())
            return ApiResponse.error("Order not found for the given order ID.");

        if (orderTrackingStageResponse.getCurrentStage().equalsIgnoreCase(TrackingStage.AT_SELLER_ADDRESS.name())) {

            String pickupDeliveryCode = driverTask.getPickupDeliveryCode();

            if (!passwordEncoder.matches(deliveryCodeVerificationRequest.getDeliveryCode(), pickupDeliveryCode))
                return ApiResponse.error("Invalid pickup delivery code. Please check the code and try again.");

            orderTrackingStageUpdateEventDto = new OrderTrackingStageUpdateEventDto(
                    deliveryCodeVerificationRequest.getOrderId(),
                    TrackingStage.PICKUP_FROM_SELLER
            );

            trackingStageNotificationEventDto = new TrackingStageNotificationEventDto(
                    orderTrackingStageResponse.getBuyerUserId(),
                    driverTask.getOrderReferenceNumber(),
                    true
            );

        } else if (orderTrackingStageResponse.getCurrentStage().equalsIgnoreCase(TrackingStage.PICKUP_FROM_SELLER.name())) {

            String dropOffDeliveryCode = driverTask.getDropoffDeliveryCode();

            if (!passwordEncoder.matches(deliveryCodeVerificationRequest.getDeliveryCode(), dropOffDeliveryCode))
                return ApiResponse.error("Invalid drop-off delivery code. Please check the code and try again.");

            orderTrackingStageUpdateEventDto = new OrderTrackingStageUpdateEventDto(
                    deliveryCodeVerificationRequest.getOrderId(),
                    TrackingStage.DELIVERED_TO_BUYER_ADDRESS
            );

            trackingStageNotificationEventDto = new TrackingStageNotificationEventDto(
                    orderTrackingStageResponse.getSellerUserId(),
                    driverTask.getOrderReferenceNumber(),
                    false
            );

            driverTask.setCompleted(true);

            driver.setDriverStatus(DriverStatus.AVAILABLE);

            DeliveryCompletedEventDto deliveryCompletedEventDto = new DeliveryCompletedEventDto(
                    driver.getUserId(),
                    UUID.fromString(orderTrackingStageResponse.getBuyerUserId()),
                    driver.getEmail(),
                    driverTask.getDeliveryFee(),
                    driverTask.getOrderReferenceNumber(),
                    driverTask.getOrderId(),
                    clientRequestKey
            );

            outboxEventService.saveEvent(
                    userIdentifier.userId(),
                    DELIVERY_COMPLETED_EVENT_TYPE,
                    DELIVERY_COMPLETED_KAFKA_BINDING,
                    deliveryCompletedEventDto,
                    clientRequestKey
            );

        } else {

            return ApiResponse.error("Invalid order tracking stage for delivery code verification.");

        }

        outboxEventService.saveEvent(
                userIdentifier.userId(),
                ORDER_TRACKING_STAGE_UPDATE_EVENT_TYPE,
                ORDER_TRACKING_STAGE_UPDATE_KAFKA_BINDING,
                orderTrackingStageUpdateEventDto,
                clientRequestKey
        );

        outboxEventService.saveEvent(
                userIdentifier.userId(),
                TRACKING_STAGE_NOTIFICATION_EVENT_TYPE,
                TRACKING_STAGE_NOTIFICATION_KAFKA_BINDING,
                trackingStageNotificationEventDto,
                clientRequestKey
        );

        driverTaskRepository.save(driverTask);

        driverRepository.save(driver);

        return ApiResponse.success("Code verified successfully.");

    }

    @Override
    public ApiResponse<CursorPaginatedResponse<DriverTaskResponse>> fetchDriverTasks(FetchDriverTaskRequest fetchDriverTaskRequest) {

        UserIdentifier userIdentifier = authenticatedUserProvider.getCurrentLoggedInUser();

        Optional<Driver> optionalDriver = driverRepository.findByUserId(UUID.fromString(userIdentifier.userId()));

        if (optionalDriver.isEmpty())
            return ApiResponse.error("Driver profile not found");

        Driver driver = optionalDriver.get();

        CursorPayload cursor = decodeCursor(fetchDriverTaskRequest.getCursor());

        // Fetch pageSize + 1 to determine if there are more pages
        Pageable pageable = PageRequest.of(0, fetchDriverTaskRequest.getPageSize() + 1);

        List<DriverTask> driverTasks;

        if (cursor == null) {

            driverTasks = driverTaskRepository.findTasksFirstPage(
                    driver.getId(),
                    DriverTaskStatus.ACCEPTED,
                    pageable
            );

        } else {

            driverTasks = driverTaskRepository.findTasksAfterCursor(
                    driver.getId(),
                    DriverTaskStatus.ACCEPTED,
                    cursor.getLastCreatedAt(),
                    cursor.getLastId(),
                    pageable
            );

        }

        // Check if there are more records beyond this page
        boolean hasMore = driverTasks.size() > fetchDriverTaskRequest.getPageSize();

        if (hasMore)
            driverTasks = driverTasks.subList(0, fetchDriverTaskRequest.getPageSize());

        // Build next cursor from the last item in the current page
        String nextCursor = null;

        if (hasMore) {

            DriverTask last = driverTasks.getLast();

            nextCursor = encodeCursor(new CursorPayload(
                    last.getCreatedAt(),
                    last.getId()
            ));

        }

        // Map entities to response DTOs
        List<DriverTaskResponse> responseItems = driverTasks
                .stream()
                .map(t -> new DriverTaskResponse(
                        t.getOrderReferenceNumber(),
                        t.getPickupAddress(),
                        t.getDropoffAddress(),
                        t.getDeliveryFee(),
                        t.getCreatedAt()
                ))
                .toList();

        CursorPaginatedResponse<DriverTaskResponse> paginatedResponse =
                new CursorPaginatedResponse<>(
                        responseItems,
                        nextCursor,
                        hasMore,
                        fetchDriverTaskRequest.getPageSize()
                );

        return ApiResponse.success("Driver tasks fetched successfully", paginatedResponse);
    }

    @McpTool(name = "fetch-driver-details", description = "Fetch the details of the driver assigned to a specific order using the order reference number.")
    @Override
    public ApiResponse<DriverDetailsResponse> fetchDetailsOfDriverAssignedToOrder(@McpToolParam(description = "This is the reference number that was assigned when order was successfully created") String orderReferenceNumber) {

        Optional<DriverTask> optionalDriverTask = driverTaskRepository.findByOrderReferenceNumberWithDriver(orderReferenceNumber);

        if (optionalDriverTask.isEmpty())
            return ApiResponse.error("No driver task found for the given order reference number.");

        DriverTask driverTask = optionalDriverTask.get();

        Driver driver = driverTask.getDriver();

        DriverDetailsResponse driverDetailsResponse = new DriverDetailsResponse(driver.getFirstname(), driver.getLastname(), driver.getPhoneNumber());

        return ApiResponse.success("Driver details fetched successfully.", driverDetailsResponse);

    }
}
