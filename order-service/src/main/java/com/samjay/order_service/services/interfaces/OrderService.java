package com.samjay.order_service.services.interfaces;

import com.samjay.order_service.dtos.events.PaymentVerificationEventDto;
import com.samjay.order_service.dtos.requests.OrderApprovalRequest;
import com.samjay.order_service.dtos.requests.OrderCreationRequest;
import com.samjay.order_service.dtos.responses.ApiResponse;
import com.samjay.order_service.dtos.responses.OrderCreationResponse;
import com.samjay.order_service.dtos.responses.UnapprovedOrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    ApiResponse<OrderCreationResponse> createOrder(OrderCreationRequest orderCreationRequest, String clientRequestKey);

    ApiResponse<List<UnapprovedOrderResponse>> fetchCustomerUnApprovedOrders();

    ApiResponse<String> approveOrder(OrderApprovalRequest orderApprovalRequest, String clientRequestKey);

    ApiResponse<String> rejectOrder(UUID orderId, String clientRequestKey);

    void updateOrderStatusToPaid(PaymentVerificationEventDto paymentVerificationEventDto);
}
