package com.samjay.order_service.services.implementations;

import com.samjay.ValidateAndFetchCustomerUsernameResponse;
import com.samjay.order_service.configurations.AuthenticatedUserProvider;
import com.samjay.order_service.dtos.events.OrderApprovalEventDto;
import com.samjay.order_service.dtos.events.PaymentCompletionEventDto;
import com.samjay.order_service.dtos.events.PaymentVerificationEventDto;
import com.samjay.order_service.dtos.requests.ItemDetailsRequest;
import com.samjay.order_service.dtos.requests.OrderApprovalRequest;
import com.samjay.order_service.dtos.requests.OrderCreationRequest;
import com.samjay.order_service.dtos.responses.*;
import com.samjay.order_service.entities.ItemDetails;
import com.samjay.order_service.entities.Order;
import com.samjay.order_service.entities.OrderDeliveryInformation;
import com.samjay.order_service.entities.OrderParticipantInformation;
import com.samjay.order_service.enumerations.OrderCreator;
import com.samjay.order_service.enumerations.OrderStatus;
import com.samjay.order_service.enumerations.PaymentStatus;
import com.samjay.order_service.exceptions.ApplicationException;
import com.samjay.order_service.repositories.OrderRepository;
import com.samjay.order_service.services.grpcservice.CustomerGrpcService;
import com.samjay.order_service.services.interfaces.*;
import com.samjay.order_service.utility.AppExtensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.samjay.order_service.utility.AppExtensions.*;

@SuppressWarnings("ALL")
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImplementation implements OrderService {

    private final OrderRepository orderRepository;

    private final IdempotencyService idempotencyService;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    private final MediaUploadService mediaUploadService;

    private final OrderPersistenceService orderPersistenceService;

    private final CustomerGrpcService customerGrpcService;

    private final CacheService cacheService;

    private final OutboxEventService outboxEventService;

    private final GoogleMapService googleMapService;

    @Transactional
    @Override
    public ApiResponse<OrderCreationResponse> createOrder(OrderCreationRequest orderCreationRequest, String clientRequestKey) {

        UserIdentifier userIdentifier = authenticatedUserProvider.getCurrentLoggedInUser();

        String fingerPrint = serialize(orderCreationRequest);

        String hashedFingerPrint = generateHash(Objects.requireNonNull(fingerPrint));

        Optional<ApiResponse<OrderCreationResponse>> existingResponse = idempotencyService.checkKey(
                clientRequestKey,
                AppExtensions.ORDER_CREATION_EVENT_TYPE,
                hashedFingerPrint,
                OrderCreationResponse.class
        );

        if (existingResponse.isPresent())
            return existingResponse.get();

        ApiResponse<ValidateAndFetchCustomerUsernameResponse> customerValidationResponse = customerGrpcService
                .validateCustomerUsername(orderCreationRequest.getUsername());

        if (!customerValidationResponse.isSuccessful())
            return ApiResponse.error(customerValidationResponse.getResponseMessage());

        ValidateAndFetchCustomerUsernameResponse customerResponse = customerValidationResponse.getResponseBody();

        boolean isUsernameValid = customerResponse.getIsUsernameValid();

        if (!isUsernameValid)
            return ApiResponse.error("Username does not exist. Please ensure the username is correct and try again.");

        if (orderCreationRequest.getUsername().equals(userIdentifier.username()))
            return ApiResponse.error("The provided username must not be the same as the username associated with the authenticated user.");

        String orderReferenceNumber = generateOrderReferenceNumber();

        int inserted = idempotencyService.saveKey(
                clientRequestKey,
                userIdentifier.userId(),
                ORDER_CREATION_EVENT_TYPE,
                hashedFingerPrint
        );

        if (inserted == 0)
            return ApiResponse.success("Another request with the same client request key is currently being processed.");

        List<ItemDetails> itemDetailsList = uploadItemMedia(orderCreationRequest.getItemDetails(), orderReferenceNumber);

        if (itemDetailsList.isEmpty())
            return ApiResponse.error("Failed to upload item media. Please try again.");

        try {

            return orderPersistenceService.persistOrder(
                    orderCreationRequest,
                    clientRequestKey,
                    userIdentifier,
                    hashedFingerPrint,
                    orderReferenceNumber,
                    itemDetailsList,
                    customerResponse
            );

        } catch (Exception e) {

            log.error("Order persistence failed, rolling back {} uploaded blobs", itemDetailsList.size(), e);

            itemDetailsList.forEach(item -> {

                try {

                    mediaUploadService.delete(item.getPhotoOrVideoUrl());

                } catch (Exception deleteEx) {

                    log.error("Failed to delete blob: {}", item.getPhotoOrVideoUrl(), deleteEx);
                }
            });

            throw new ApplicationException(
                    "An unexpected error occurred while creating the order. Please try again later.",
                    HttpStatus.BAD_REQUEST
            );

        }
    }

    @Override
    public ApiResponse<List<UnapprovedOrderResponse>> fetchCustomerUnApprovedOrders() {

        UserIdentifier userIdentifier = authenticatedUserProvider.getCurrentLoggedInUser();

        UUID userId = UUID.fromString(userIdentifier.userId());

        String cacheKey = UNAPPROVED_ORDER_CACHE_KEY_PREFIX + userId;

        Optional<List> cached = cacheService.get(cacheKey, List.class);

        if (cached.isPresent()) {

            log.info("Cache hit for user {}", userId);

            return ApiResponse.success("Unapproved orders fetched successfully.", (List<UnapprovedOrderResponse>) cached.get());
        }

        log.info("Cache miss for user {}", userId);

        List<Order> orders = orderRepository.findByOrderStatusAndReviewerUserId(OrderStatus.UNAPPROVED, userId);

        if (orders.isEmpty())
            return ApiResponse.success("No unapproved orders found.", Collections.emptyList());

        List<UnapprovedOrderResponse> response = orders
                .stream()
                .map(this::mapToUnapprovedOrderResponse)
                .toList();

        cacheService.set(cacheKey, response, Duration.ofMinutes(10));

        return ApiResponse.success("Unapproved orders fetched successfully.", response);

        //Todo Implement cursor based pagination for this endpoint to enhance performance and scalability as the number of unapproved orders grows.
    }

    @Transactional
    @Override
    public ApiResponse<String> approveOrder(OrderApprovalRequest orderApprovalRequest, String clientRequestKey) {

        UserIdentifier userIdentifier = authenticatedUserProvider.getCurrentLoggedInUser();

        Optional<Order> optionalOrder = orderRepository.findById(orderApprovalRequest.getOrderId());

        if (optionalOrder.isEmpty())
            return ApiResponse.error("Order not found with the provided ID.");

        Order order = optionalOrder.get();

        if (userIdentifier.userId().equals(order.getCreatorUserId().toString()))
            return ApiResponse.error("The creator of the order cannot approve their own order.");

        OrderCreator orderCreator = order.getOrderCreator();

        OrderParticipantInformation participantInformation = order.getParticipantInformation();

        OrderDeliveryInformation deliveryInformation = order.getDeliveryInformation();

        OrderApprovalEventDto orderApprovalEventDto;

        ApiResponse<LatitudeAndLongitudeResponse> latitudeAndLongitudeResponseApiResponse = googleMapService
                .getLatitudeAndLongitudeFromAddress(orderApprovalRequest.getAddress());

        if (!latitudeAndLongitudeResponseApiResponse.isSuccessful())
            return ApiResponse.error("Failed to fetch geo-coordinates for the provided address. Please ensure the address is valid and try again.");

        ApiResponse<DistanceAndDurationResponse> distanceAndDurationResponseApiResponse = googleMapService
                .getDurationAndDistanceBetweenAddresses(
                        orderCreator == OrderCreator.BUYER ? participantInformation.getDropOffAddress() : participantInformation.getPickupAddress(),
                        orderApprovalRequest.getAddress()
                );

        if (!distanceAndDurationResponseApiResponse.isSuccessful())
            return ApiResponse.error("Failed to fetch distance and duration for the provided addresses. Please ensure the addresses are valid and try again.");

        switch (orderCreator) {

            case OrderCreator.BUYER -> {

                participantInformation.setPickupState(orderApprovalRequest.getState());

                participantInformation.setPickupAddress(orderApprovalRequest.getAddress());

                participantInformation.setSellerUserId(userIdentifier.userId());

                participantInformation.setSellerUsername(userIdentifier.username());

                participantInformation.setSellerEmail(userIdentifier.email());

                participantInformation.setSellerPhoneNumber(orderApprovalRequest.getPhoneNumber());

                deliveryInformation.setPickupAddressLatitude(latitudeAndLongitudeResponseApiResponse.getResponseBody().latitude());

                deliveryInformation.setPickupAddressLongitude(latitudeAndLongitudeResponseApiResponse.getResponseBody().longitude());

                orderApprovalEventDto = new OrderApprovalEventDto(
                        participantInformation.getBuyerEmail(),
                        userIdentifier.email(),
                        order.getOrderReferenceNumber(),
                        participantInformation.getBuyerUsername(),
                        userIdentifier.username(),
                        orderCreator
                );
            }
            case OrderCreator.SELLER -> {

                participantInformation.setDropOffState(orderApprovalRequest.getState());

                participantInformation.setDropOffAddress(orderApprovalRequest.getAddress());

                participantInformation.setBuyerUserId(userIdentifier.userId());

                participantInformation.setBuyerUsername(userIdentifier.username());

                participantInformation.setBuyerEmail(userIdentifier.email());

                participantInformation.setBuyerPhoneNumber(orderApprovalRequest.getPhoneNumber());

                deliveryInformation.setDropOffAddressLatitude(latitudeAndLongitudeResponseApiResponse.getResponseBody().latitude());

                deliveryInformation.setDropOffAddressLongitude(latitudeAndLongitudeResponseApiResponse.getResponseBody().longitude());

                orderApprovalEventDto = new OrderApprovalEventDto(
                        userIdentifier.email(),
                        participantInformation.getSellerEmail(),
                        order.getOrderReferenceNumber(),
                        userIdentifier.username(),
                        participantInformation.getSellerUsername(),
                        orderCreator
                );
            }
            default -> {

                return ApiResponse.error("Invalid order creator type. The order creator must be either BUYER or SELLER.");
            }
        }

        deliveryInformation.setDistanceInKm(distanceAndDurationResponseApiResponse.getResponseBody().distance());

        deliveryInformation.setEstimatedDeliveryTime(distanceAndDurationResponseApiResponse.getResponseBody().duration());

        deliveryInformation.setDeliveryFee(100 * distanceAndDurationResponseApiResponse.getResponseBody().distance());

        order.setOrderStatus(OrderStatus.APPROVED);

        orderRepository.save(order);

        String cacheKey = AppExtensions.UNAPPROVED_ORDER_CACHE_KEY_PREFIX + userIdentifier.userId();

        cacheService.delete(cacheKey);

        outboxEventService.saveEvent(
                userIdentifier.userId(),
                ORDER_APPROVAL_EVENT_TYPE,
                ORDER_APPROVAL_KAFKA_BINDING,
                orderApprovalEventDto,
                clientRequestKey
        );

        return ApiResponse.success("Order approved successfully.");
    }

    @Override
    public ApiResponse<String> rejectOrder(UUID orderId, String clientRequestKey) {
        return null;
    }

    @Transactional
    @Override
    public void updateOrderStatusToPaid(PaymentVerificationEventDto paymentVerificationEventDto) {

        try {

            Order order = orderRepository
                    .findByIdWithDetails(paymentVerificationEventDto.orderId())
                    .orElseThrow(() -> new ApplicationException(
                            "Order not found for ID: " + paymentVerificationEventDto.orderId(),
                            HttpStatus.BAD_REQUEST)
                    );

            if (order.getPaymentStatus() == PaymentStatus.PAID) {

                log.warn("Order with ID {} is already marked as PAID. Skipping update.", order.getId());

                return;
            }

            order.setPaymentStatus(PaymentStatus.PAID);

            order.setOrderStatus(OrderStatus.IN_PROGRESS);

            Order savedOrder = orderRepository.save(order);

            PaymentCompletionEventDto paymentCompletionEventDto = new PaymentCompletionEventDto(
                    savedOrder.getParticipantInformation().getBuyerEmail(),
                    savedOrder.getParticipantInformation().getSellerEmail(),
                    UUID.fromString(savedOrder.getParticipantInformation().getBuyerUserId()),
                    UUID.fromString(savedOrder.getParticipantInformation().getSellerUserId()),
                    savedOrder.getDeliveryInformation().getPickupAddressLatitude(),
                    savedOrder.getDeliveryInformation().getPickupAddressLongitude(),
                    savedOrder.getDeliveryInformation().getDeliveryFee(),
                    savedOrder.getParticipantInformation().getPickupAddress() + ", " + savedOrder.getParticipantInformation().getPickupState(),
                    savedOrder.getParticipantInformation().getDropOffAddress() + ", " + savedOrder.getParticipantInformation().getDropOffState(),
                    savedOrder.getOrderReferenceNumber(),
                    paymentVerificationEventDto.amount(),
                    paymentVerificationEventDto.paymentId(),
                    paymentVerificationEventDto.orderId(),
                    paymentVerificationEventDto.clientRequestKey()
            );

            outboxEventService.saveEvent(
                    savedOrder.getParticipantInformation().getBuyerUserId(),
                    PAYMENT_COMPLETION_EVENT_TYPE,
                    PAYMENT_COMPLETION_KAFKA_BINDING,
                    paymentCompletionEventDto,
                    paymentVerificationEventDto.clientRequestKey()
            );

        } catch (Exception e) {

            log.error("Error updating order status to paid for order ID {}: {}", paymentVerificationEventDto.orderId(), e.getMessage(), e);

            throw e;
        }
    }

    private String generateOrderReferenceNumber() {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        int random = ThreadLocalRandom.current().nextInt(1000, 9999);

        return "ORD-" + timestamp + "-" + random;
    }

    private List<ItemDetails> uploadItemMedia(List<ItemDetailsRequest> itemDetailsRequests, String orderReferenceNumber) {

        List<ItemDetails> itemDetailsList = new ArrayList<>();

        List<String> uploadedUrls = new ArrayList<>();

        try {

            for (ItemDetailsRequest itemRequest : itemDetailsRequests) {

                UploadResult result = mediaUploadService.upload(itemRequest.getItemMedia(), orderReferenceNumber);

                uploadedUrls.add(result.url());

                ItemDetails itemDetails = mapToItemDetails(itemRequest);

                itemDetails.setPhotoOrVideoUrl(result.url());

                itemDetails.setMediaType(result.mediaType());

                itemDetailsList.add(itemDetails);

            }

            return itemDetailsList;

        } catch (Exception e) {

            uploadedUrls.forEach(mediaUploadService::delete);

            itemDetailsList.clear();

            return itemDetailsList;

        }
    }

    private ItemDetails mapToItemDetails(ItemDetailsRequest itemDetailsRequest) {

        ItemDetails itemDetails = new ItemDetails();

        itemDetails.setName(itemDetailsRequest.getName());

        itemDetails.setDescription(itemDetailsRequest.getDescription());

        itemDetails.setQuantity(itemDetailsRequest.getQuantity());

        itemDetails.setPrice(itemDetailsRequest.getPrice());

        return itemDetails;
    }

    private UnapprovedOrderResponse mapToUnapprovedOrderResponse(Order order) {

        OrderParticipantInformation orderParticipantInformation = order.getParticipantInformation();

        String username = order.getOrderCreator() == OrderCreator.BUYER ? orderParticipantInformation.getBuyerUsername()
                : orderParticipantInformation.getSellerUsername();

        UnapprovedOrderResponse response = new UnapprovedOrderResponse();

        response.setOrderId(order.getId());

        response.setOrderReferenceNumber(order.getOrderReferenceNumber());

        response.setOrderStatus(order.getOrderStatus());

        response.setPaymentStatus(order.getPaymentStatus());

        response.setCreatorUsername(username);

        response.setCreatedAt(order.getCreatedAt());

        response.setItemDetails(order.getItemDetails().stream().map(item -> {

            ItemDetailsResponse itemResponse = new ItemDetailsResponse();

            itemResponse.setName(item.getName());

            itemResponse.setDescription(item.getDescription());

            itemResponse.setQuantity(item.getQuantity());

            itemResponse.setPrice(item.getPrice());

            itemResponse.setPhotoOrVideoUrl(item.getPhotoOrVideoUrl());

            return itemResponse;

        }).toList());

        return response;
    }
}
