package com.samjay.order_service.services.grpcservice;

import com.samjay.ValidateAndFetchCustomerUsernameResponse;
import com.samjay.order_service.dtos.responses.ApiResponse;

public interface CustomerGrpcService {

    ApiResponse<ValidateAndFetchCustomerUsernameResponse> validateCustomerUsername(String username);
}
