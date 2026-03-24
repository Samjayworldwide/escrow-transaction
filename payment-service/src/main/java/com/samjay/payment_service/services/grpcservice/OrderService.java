package com.samjay.payment_service.services.grpcservice;

import com.samjay.FetchOrderDetailsResponse;
import com.samjay.payment_service.dtos.responses.ApiResponse;

public interface OrderService {

    ApiResponse<FetchOrderDetailsResponse> fetchOrderDetails(String orderId, String userId);
}
