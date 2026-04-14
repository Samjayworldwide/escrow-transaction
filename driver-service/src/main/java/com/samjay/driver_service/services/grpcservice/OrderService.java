package com.samjay.driver_service.services.grpcservice;

import com.samjay.OrderDetailsForDriverSearchResponse;
import com.samjay.OrderDetailsResponse;
import com.samjay.OrderTrackingStageResponse;
import com.samjay.driver_service.dtos.responses.ApiResponse;

import java.util.UUID;

public interface OrderService {

    ApiResponse<OrderDetailsResponse> getOrderDetails(UUID orderId);

    ApiResponse<OrderTrackingStageResponse> getOrderTrackingStage(UUID orderId);

    ApiResponse<OrderDetailsForDriverSearchResponse> getOrderDetailsForDriverSearch(UUID orderId);

}
