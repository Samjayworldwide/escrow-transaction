package com.samjay.customer_service.services.grpcimplementations;

import com.samjay.CustomerServiceGrpc;
import com.samjay.ValidateAndFetchCustomerUsernameRequest;
import com.samjay.ValidateAndFetchCustomerUsernameResponse;
import com.samjay.customer_service.entities.Customer;
import com.samjay.customer_service.repositories.CustomerRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.util.Optional;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceGrpcImplementation extends CustomerServiceGrpc.CustomerServiceImplBase {

    private final CustomerRepository customerRepository;

    @Override
    public void validateAndFetchCustomerUsername(ValidateAndFetchCustomerUsernameRequest request, StreamObserver<ValidateAndFetchCustomerUsernameResponse> responseObserver) {

        try {

            log.info("This is the request received in Customer Service: {}", request.toString());

            ValidateAndFetchCustomerUsernameResponse response;

            Optional<Customer> optionalCustomer = customerRepository.findByUsername(request.getUsername());

            if (optionalCustomer.isEmpty()) {

                response = ValidateAndFetchCustomerUsernameResponse
                        .newBuilder()
                        .setIsUsernameValid(false)
                        .setEmail("")
                        .build();

                responseObserver.onNext(response);

                responseObserver.onCompleted();

                return;
            }

            Customer customer = optionalCustomer.get();

            response = ValidateAndFetchCustomerUsernameResponse
                    .newBuilder()
                    .setIsUsernameValid(true)
                    .setEmail(customer.getEmail())
                    .setUserId(customer.getUserId().toString())
                    .build();

            responseObserver.onNext(response);

            responseObserver.onCompleted();

        } catch (Exception ex) {

            log.error("Error validating username", ex);

            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error while validating username")
                    .withCause(ex)
                    .asRuntimeException()
            );
        }
    }
}
