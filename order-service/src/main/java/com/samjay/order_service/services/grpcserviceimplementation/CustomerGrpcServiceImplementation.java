package com.samjay.order_service.services.grpcserviceimplementation;

import com.samjay.CustomerServiceGrpc;
import com.samjay.ValidateAndFetchCustomerUsernameRequest;
import com.samjay.ValidateAndFetchCustomerUsernameResponse;
import com.samjay.order_service.dtos.responses.ApiResponse;
import com.samjay.order_service.services.grpcservice.CustomerGrpcService;
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
public class CustomerGrpcServiceImplementation implements CustomerGrpcService {

    private final CustomerServiceGrpc.CustomerServiceBlockingStub customerServiceBlockingStub;

    @CircuitBreaker(name = "interServiceCircuit", fallbackMethod = "fallbackValidateCustomerUsername")
    @Retry(name = "interServiceRetry", fallbackMethod = "fallbackValidateCustomerUsername")
    @Bulkhead(name = "interServiceBulkhead", fallbackMethod = "fallbackValidateCustomerUsername", type = Bulkhead.Type.SEMAPHORE)
    @Override
    public ApiResponse<ValidateAndFetchCustomerUsernameResponse> validateCustomerUsername(String username) {

        ValidateAndFetchCustomerUsernameRequest customerRequest = ValidateAndFetchCustomerUsernameRequest
                .newBuilder()
                .setUsername(username)
                .build();

        ValidateAndFetchCustomerUsernameResponse customerResponse = customerServiceBlockingStub
                .withDeadlineAfter(2, TimeUnit.SECONDS)
                .validateAndFetchCustomerUsername(customerRequest);

        return ApiResponse.success("Customer username validated successfully.", customerResponse);

    }

    public ApiResponse<ValidateAndFetchCustomerUsernameResponse> fallbackValidateCustomerUsername(String username, Throwable throwable) {

        String reason = (throwable != null) ? throwable.getMessage() : "Unknown reason";

        log.error("Fallback executed for validate customer username with username: {} due to: {}", username, reason);

        return ApiResponse.error("Unable to validate username provided at this time. Please try again later.");
    }
}
