package com.samjay.order_service.services.grpcserviceimplementation;

import com.samjay.FetchOrderDetailsRequest;
import com.samjay.FetchOrderDetailsResponse;
import com.samjay.OrderServiceGrpc;
import com.samjay.order_service.entities.Order;
import com.samjay.order_service.enumerations.OrderStatus;
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
                        .setTotalPriceOfItems(0)
                        .setOrderRefrenceNumber("")
                        .build();

                responseObserver.onNext(response);

                responseObserver.onCompleted();

                return;
            }

            Order order = optionalOrder.get();

            boolean isApproved = order.getOrderStatus() == OrderStatus.APPROVED;

            boolean isPaidByBuyer = order.getParticipantInformation().getBuyerUserId().equals(request.getUserId());

            BigDecimal totalPriceOfItems = order.getItemDetails().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            response = FetchOrderDetailsResponse
                    .newBuilder()
                    .setIsFound(true)
                    .setIsApproved(isApproved)
                    .setIsPaidByBuyer(isPaidByBuyer)
                    .setTotalPriceOfItems(totalPriceOfItems.doubleValue())
                    .setOrderRefrenceNumber(order.getOrderReferenceNumber())
                    .build();

            responseObserver.onNext(response);

            responseObserver.onCompleted();

        } catch (Exception e) {

            log.error("Error while logging request details: {}", e.getMessage());

            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error while validating username")
                    .withCause(e)
                    .asRuntimeException()
            );
        }
    }
}
