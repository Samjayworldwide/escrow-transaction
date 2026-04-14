package com.samjay.order_service.services.grpcserviceimplementation;

import com.samjay.*;
import com.samjay.FetchOrderDetailsRequest;
import com.samjay.FetchOrderDetailsResponse;
import com.samjay.OrderDetailsRequest;
import com.samjay.OrderDetailsResponse;
import com.samjay.OrderServiceGrpc;
import com.samjay.OrderTrackingStageRequest;
import com.samjay.OrderTrackingStageResponse;
import com.samjay.order_service.entities.Order;
import com.samjay.order_service.enumerations.OrderStatus;
import com.samjay.order_service.enumerations.PaymentStatus;
import com.samjay.order_service.repositories.OrderRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class OrderServiceGrpcImplementation extends OrderServiceGrpc.OrderServiceImplBase {

    private final OrderRepository orderRepository;

    @Override
    public void fetchOrderDetails(FetchOrderDetailsRequest request, StreamObserver<FetchOrderDetailsResponse> responseObserver) {

        try {

            log.info("Received request to fetch order details for order ID: {} and user ID: {}", request.getOrderId(), request.getUserId());

            FetchOrderDetailsResponse response;

            Optional<Order> optionalOrder = orderRepository.findByIdWithDetails(UUID.fromString(request.getOrderId()));

            if (optionalOrder.isEmpty()) {

                response = FetchOrderDetailsResponse
                        .newBuilder()
                        .setIsFound(false)
                        .setIsApproved(false)
                        .setIsPaidByBuyer(false)
                        .setIsOrderAlreadyPaid(false)
                        .setTotalPriceOfItems(0)
                        .setOrderRefrenceNumber("")
                        .build();

                responseObserver.onNext(response);

                responseObserver.onCompleted();

                return;
            }

            Order order = optionalOrder.get();

            boolean isUnApproved = order.getOrderStatus() == OrderStatus.UNAPPROVED;

            boolean isOrderAlreadyPaid = order.getPaymentStatus() == PaymentStatus.PAID;

            boolean isPaidByBuyer = order.getParticipantInformation().getBuyerUserId().equals(request.getUserId());

            BigDecimal totalPriceOfItems = order
                    .getItemDetails()
                    .stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal deliveryFee = order.getDeliveryInformation().getDeliveryFee();

            BigDecimal totalCost = totalPriceOfItems.add(deliveryFee);

            response = FetchOrderDetailsResponse
                    .newBuilder()
                    .setIsFound(true)
                    .setIsApproved(isUnApproved)
                    .setIsPaidByBuyer(isPaidByBuyer)
                    .setTotalPriceOfItems(totalCost.doubleValue())
                    .setOrderRefrenceNumber(order.getOrderReferenceNumber())
                    .setIsOrderAlreadyPaid(isOrderAlreadyPaid)
                    .build();

            responseObserver.onNext(response);

            responseObserver.onCompleted();

        } catch (Exception e) {

            log.error("Error while logging request details: {}", e.getMessage());

            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error while fetching order details")
                    .withCause(e)
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void getOrderDetails(OrderDetailsRequest request, StreamObserver<OrderDetailsResponse> responseObserver) {

        try {

            log.info("Received request to fetch order participant details for order ID: {}", request.getOrderId());

            OrderDetailsResponse response;

            Optional<Order> optionalOrder = orderRepository.findOrderByIdWithDeliveryInformationAndParticipantInformation(UUID.fromString(request.getOrderId()));

            if (optionalOrder.isEmpty()) {

                response = OrderDetailsResponse
                        .newBuilder()
                        .setIsFound(false)
                        .build();

                responseObserver.onNext(response);

                responseObserver.onCompleted();

                return;
            }

            Order order = optionalOrder.get();

            response = OrderDetailsResponse
                    .newBuilder()
                    .setIsFound(true)
                    .setOrderReferenceNumber(order.getOrderReferenceNumber())
                    .setBuyerUserId(order.getParticipantInformation().getBuyerUserId())
                    .setSellerUserId(order.getParticipantInformation().getSellerUserId())
                    .setBuyerEmail(order.getParticipantInformation().getBuyerEmail())
                    .setSellerEmail(order.getParticipantInformation().getSellerEmail())
                    .setPickupAddress(order.getParticipantInformation().getPickupAddress())
                    .setDropOffAddress(order.getParticipantInformation().getDropOffAddress())
                    .setDeliveryFee(order.getDeliveryInformation().getDeliveryFee().doubleValue())
                    .build();

            responseObserver.onNext(response);

            responseObserver.onCompleted();

        } catch (Exception ex) {

            log.error("Error while logging request details: {}", ex.getMessage());

            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error while getting order participant details")
                    .withCause(ex)
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void getOrderTrackingStage(OrderTrackingStageRequest request, StreamObserver<OrderTrackingStageResponse> responseObserver) {

        try {

            log.info("Received request to fetch order tracking stage for order ID: {}", request.getOrderId());

            OrderTrackingStageResponse response;

            Optional<Order> optionalOrder = orderRepository.findByIdWithParticipantInformation(UUID.fromString(request.getOrderId()));

            if (optionalOrder.isEmpty()) {

                response = OrderTrackingStageResponse
                        .newBuilder()
                        .setIsFound(false)
                        .setCurrentStage("")
                        .setSellerUserId("")
                        .setBuyerUserId("")
                        .setOrderRefrenceNumber("")
                        .build();

                responseObserver.onNext(response);

                responseObserver.onCompleted();

                return;
            }

            Order order = optionalOrder.get();

            response = OrderTrackingStageResponse
                    .newBuilder()
                    .setIsFound(true)
                    .setCurrentStage(order.getTrackingStage().name())
                    .setOrderRefrenceNumber(order.getOrderReferenceNumber())
                    .setBuyerUserId(order.getParticipantInformation().getBuyerUserId())
                    .setSellerUserId(order.getParticipantInformation().getSellerUserId())
                    .build();

            responseObserver.onNext(response);

            responseObserver.onCompleted();

        } catch (Exception ex) {

            log.error("Error while logging request details: {}", ex.getMessage());

            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error while getting order tracking stage")
                    .withCause(ex)
                    .asRuntimeException()
            );
        }
    }
}
