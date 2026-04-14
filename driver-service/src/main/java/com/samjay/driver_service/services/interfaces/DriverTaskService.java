package com.samjay.driver_service.services.interfaces;

import com.samjay.driver_service.dtos.requests.DeliveryCodeVerificationRequest;
import com.samjay.driver_service.dtos.requests.FetchDriverTaskRequest;
import com.samjay.driver_service.dtos.responses.ApiResponse;
import com.samjay.driver_service.dtos.responses.CursorPaginatedResponse;
import com.samjay.driver_service.dtos.responses.DriverTaskResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface DriverTaskService {

    int createDriverTaskIgnoreConflict(UUID orderId,
                                       String orderReferenceNumber,
                                       BigDecimal deliveryFee,
                                       String pickupAddress,
                                       String dropoffAddress,
                                       String pickupCode,
                                       String dropoffCode,
                                       UUID driverId

    );

    boolean isOrderAccepted(UUID orderId);

    ApiResponse<String> verifyDeliveryCode(String clientRequestKey, DeliveryCodeVerificationRequest deliveryCodeVerificationRequest);

    ApiResponse<CursorPaginatedResponse<DriverTaskResponse>> fetchDriverTasks(FetchDriverTaskRequest fetchDriverTaskRequest);

}
