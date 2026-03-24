package com.samjay.payment_service.services.grpcserviceimplementations;

import com.samjay.FetchOrderDetailsRequest;
import com.samjay.FetchOrderDetailsResponse;
import com.samjay.OrderServiceGrpc;
import com.samjay.payment_service.dtos.responses.ApiResponse;
import com.samjay.payment_service.services.grpcservice.OrderService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImplementation implements OrderService {

    private final OrderServiceGrpc.OrderServiceBlockingStub orderServiceBlockingStub;

    @CircuitBreaker(name = "interServiceCircuit", fallbackMethod = "fallbackfetchOrderDetails")
    @Retry(name = "interServiceRetry", fallbackMethod = "fallbackfetchOrderDetails")
    @Bulkhead(name = "interServiceBulkhead", fallbackMethod = "fallbackfetchOrderDetails", type = Bulkhead.Type.SEMAPHORE)
    @Override
    public ApiResponse<FetchOrderDetailsResponse> fetchOrderDetails(String orderId, String userId) {

        FetchOrderDetailsRequest fetchOrderDetailsRequest = FetchOrderDetailsRequest
                .newBuilder()
                .setOrderId(orderId)
                .setUserId(userId)
                .build();

        FetchOrderDetailsResponse fetchOrderDetailsResponse = orderServiceBlockingStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .fetchOrderDetails(fetchOrderDetailsRequest);

        return ApiResponse.success("Order items amount fetched successfully.", fetchOrderDetailsResponse);

    }

    public ApiResponse<FetchOrderDetailsResponse> fallbackfetchOrderDetails(String orderId, String userId, Throwable throwable) {

        String reason = (throwable != null) ? throwable.getMessage() : "Unknown reason";

        log.error("Fallback executed for fetch order items amount with order ID: {} due to: {}", orderId, reason);

        return ApiResponse.error("Unable to fetch order items amount at this time. Please try again later.");
    }
}
