package com.samjay.order_service.services.implementations;

import com.samjay.ValidateAndFetchCustomerUsernameResponse;
import com.samjay.order_service.dtos.events.OrderCreationEventDto;
import com.samjay.order_service.dtos.requests.OrderCreationRequest;
import com.samjay.order_service.dtos.responses.ApiResponse;
import com.samjay.order_service.dtos.responses.OrderCreationResponse;
import com.samjay.order_service.dtos.responses.UserIdentifier;
import com.samjay.order_service.entities.ItemDetails;
import com.samjay.order_service.entities.Order;
import com.samjay.order_service.entities.OrderParticipantInformation;
import com.samjay.order_service.enumerations.OrderCreator;
import com.samjay.order_service.repositories.OrderRepository;
import com.samjay.order_service.services.interfaces.CacheService;
import com.samjay.order_service.services.interfaces.IdempotencyService;
import com.samjay.order_service.services.interfaces.OrderPersistenceService;
import com.samjay.order_service.services.interfaces.OutboxEventService;
import com.samjay.order_service.utility.AppExtensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.samjay.order_service.utility.AppExtensions.ORDER_CREATION_EVENT_TYPE;
import static com.samjay.order_service.utility.AppExtensions.ORDER_CREATION_KAFKA_BINDING;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPersistenceServiceImplementation implements OrderPersistenceService {

    private final OrderRepository orderRepository;

    private final IdempotencyService idempotencyService;

    private final OutboxEventService outboxEventService;

    private final CacheService cacheService;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ApiResponse<OrderCreationResponse> persistOrder(OrderCreationRequest orderCreationRequest, String clientRequestKey,
                                                           UserIdentifier userIdentifier, String hashedFingerPrint,
                                                           String orderReferenceNumber, List<ItemDetails> itemDetailsList,
                                                           ValidateAndFetchCustomerUsernameResponse validateAndFetchCustomerUsernameResponse) {

        Order order = new Order();

        order.setOrderReferenceNumber(orderReferenceNumber);

        order.setCreatorUserId(UUID.fromString(userIdentifier.userId()));

        order.setReviewerUserId(UUID.fromString(validateAndFetchCustomerUsernameResponse.getUserId()));

        OrderParticipantInformation participantInformation = new OrderParticipantInformation();

        if (orderCreationRequest.getOrderCreator() == OrderCreator.BUYER) {

            participantInformation.setBuyerUserId(userIdentifier.userId());

            participantInformation.setBuyerEmail(userIdentifier.email());

            participantInformation.setBuyerPhoneNumber(orderCreationRequest.getPhoneNumber());

            participantInformation.setBuyerUsername(userIdentifier.username());

            participantInformation.setDropOffState(orderCreationRequest.getState());

            participantInformation.setDropOffAddress(orderCreationRequest.getAddress());

            order.setOrderCreator(OrderCreator.BUYER);

        } else if (orderCreationRequest.getOrderCreator() == OrderCreator.SELLER) {

            participantInformation.setSellerUserId(userIdentifier.userId());

            participantInformation.setSellerEmail(userIdentifier.email());

            participantInformation.setSellerPhoneNumber(orderCreationRequest.getPhoneNumber());

            participantInformation.setSellerUsername(userIdentifier.username());

            participantInformation.setPickupState(orderCreationRequest.getState());

            participantInformation.setPickupAddress(orderCreationRequest.getAddress());

            order.setOrderCreator(OrderCreator.SELLER);

        } else {

            return ApiResponse.error("Invalid order creator type. The order creator must be either BUYER or SELLER.");
        }

        order.setParticipantInformation(participantInformation);

        itemDetailsList.forEach(order::addItemDetails);

        Order savedOrder = orderRepository.save(order);

        OrderCreationEventDto orderCreationEventDto = new OrderCreationEventDto(
                validateAndFetchCustomerUsernameResponse.getEmail(),
                orderReferenceNumber,
                userIdentifier.username(),
                orderCreationRequest.getOrderCreator()
        );

        outboxEventService.saveEvent(
                userIdentifier.userId(),
                ORDER_CREATION_EVENT_TYPE,
                ORDER_CREATION_KAFKA_BINDING,
                orderCreationEventDto,
                clientRequestKey
        );

        OrderCreationResponse orderCreationResponse = OrderCreationResponse
                .builder()
                .orderId(savedOrder.getId())
                .orderReferenceNumber(savedOrder.getOrderReferenceNumber())
                .build();

        idempotencyService.markKeyAsSuccess(
                clientRequestKey,
                ORDER_CREATION_EVENT_TYPE,
                "Order created successfully",
                orderCreationResponse
        );

        String cacheKey = AppExtensions.UNAPPROVED_ORDER_CACHE_KEY_PREFIX + validateAndFetchCustomerUsernameResponse.getUserId();

        cacheService.delete(cacheKey);

        return ApiResponse.success("Order created successfully", orderCreationResponse);
    }
}
