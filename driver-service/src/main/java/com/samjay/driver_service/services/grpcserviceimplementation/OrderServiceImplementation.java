package com.samjay.driver_service.services.grpcserviceimplementation;

import com.samjay.*;
import com.samjay.driver_service.dtos.responses.ApiResponse;
import com.samjay.driver_service.services.grpcservice.OrderService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImplementation implements OrderService {

    private final OrderServiceGrpc.OrderServiceBlockingStub orderServiceBlockingStub;

    @CircuitBreaker(name = "interServiceCircuit", fallbackMethod = "fallbackGetOrderDetails")
    @Retry(name = "interServiceRetry", fallbackMethod = "fallbackGetOrderDetails")
    @Bulkhead(name = "interServiceBulkhead", fallbackMethod = "fallbackGetOrderDetails", type = Bulkhead.Type.SEMAPHORE)
    @Override
    public ApiResponse<OrderDetailsResponse> getOrderDetails(UUID orderId) {

        try {

            OrderDetailsRequest getOrderParticipantDetailsRequest = OrderDetailsRequest
                    .newBuilder()
                    .setOrderId(orderId.toString())
                    .build();

            OrderDetailsResponse getOrderParticipantDetailsResponse = orderServiceBlockingStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .getOrderDetails(getOrderParticipantDetailsRequest);

            return ApiResponse.success("Order participant details fetched successfully.", getOrderParticipantDetailsResponse);

        } catch (Exception ex) {

            log.error("Error while fetching order participant details for order ID: {}, Exception message: {}",
                    orderId,
                    ex.getMessage(),
                    ex
            );

            throw ex;
        }
    }

    @CircuitBreaker(name = "interServiceCircuit", fallbackMethod = "fallbackGetOrderTrackingStage")
    @Retry(name = "interServiceRetry", fallbackMethod = "fallbackGetOrderTrackingStage")
    @Bulkhead(name = "interServiceBulkhead", fallbackMethod = "fallbackGetOrderTrackingStage", type = Bulkhead.Type.SEMAPHORE)
    @Override
    public ApiResponse<OrderTrackingStageResponse> getOrderTrackingStage(UUID orderId) {

        try {

            OrderTrackingStageRequest orderTrackingStageRequest = OrderTrackingStageRequest
                    .newBuilder()
                    .setOrderId(orderId.toString())
                    .build();

            OrderTrackingStageResponse orderTrackingStageResponse = orderServiceBlockingStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .getOrderTrackingStage(orderTrackingStageRequest);

            return ApiResponse.success("Order tracking stage fetched successfully.", orderTrackingStageResponse);

        } catch (Exception ex) {

            log.error("Error while fetching order tracking stage for order ID: {}, Exception message: {}",
                    orderId,
                    ex.getMessage(),
                    ex
            );

            throw ex;
        }
    }

    @CircuitBreaker(name = "interServiceCircuit", fallbackMethod = "fallbackGetOrderDetailsForDriverSearch")
    @Retry(name = "interServiceRetry", fallbackMethod = "fallbackGetOrderDetailsForDriverSearch")
    @Bulkhead(name = "interServiceBulkhead", fallbackMethod = "fallbackGetOrderDetailsForDriverSearch", type = Bulkhead.Type.SEMAPHORE)
    @Override
    public ApiResponse<OrderDetailsForDriverSearchResponse> getOrderDetailsForDriverSearch(UUID orderId) {

        try {

            OrderDetailsForDriverSearchRequest orderDetailsForDriverSearchRequest = OrderDetailsForDriverSearchRequest
                    .newBuilder()
                    .setOrderId(orderId.toString())
                    .build();

            OrderDetailsForDriverSearchResponse orderDetailsForDriverSearchResponse = orderServiceBlockingStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .getOrderDetailsForDriverSearch(orderDetailsForDriverSearchRequest);

            return ApiResponse.success("Order details for driver search fetched successfully.", orderDetailsForDriverSearchResponse);

        } catch (Exception ex) {

            log.error("Error while fetching order details for driver search for order ID: {}, Exception message: {}",
                    orderId,
                    ex.getMessage(),
                    ex
            );

            throw ex;
        }
    }

    public ApiResponse<OrderDetailsResponse> fallbackGetOrderDetails(UUID orderId, Throwable throwable) {

        String reason = (throwable != null) ? throwable.getMessage() : "Unknown reason";

        log.error("Fallback executed for fetching order details with order ID: {} due to: {}", orderId, reason);

        return ApiResponse.error("Unable to fetch order details Please try again later.");
    }

    public ApiResponse<OrderTrackingStageResponse> fallbackGetOrderTrackingStage(UUID orderId, Throwable throwable) {

        String reason = (throwable != null) ? throwable.getMessage() : "Unknown reason";

        log.error("Fallback executed for fetching order tracking stage with order ID: {} due to: {}", orderId, reason);

        return ApiResponse.error("Unable to fetch order tracking stage. Please try again later.");
    }

    public ApiResponse<OrderDetailsForDriverSearchResponse> fallbackGetOrderDetailsForDriverSearch(UUID orderId, Throwable throwable) {

        String reason = (throwable != null) ? throwable.getMessage() : "Unknown reason";

        log.error("Fallback executed for fetching order details for driver search with order ID: {} due to: {}", orderId, reason);

        return ApiResponse.error("Unable to fetch order details for driver search. Please try again later.");
    }
}
