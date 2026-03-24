package com.samjay.order_service.controllers;

import com.samjay.order_service.dtos.requests.OrderApprovalRequest;
import com.samjay.order_service.dtos.requests.OrderCreationRequest;
import com.samjay.order_service.dtos.responses.ApiResponse;
import com.samjay.order_service.dtos.responses.OrderCreationResponse;
import com.samjay.order_service.dtos.responses.UnapprovedOrderResponse;
import com.samjay.order_service.services.interfaces.OrderService;
import com.samjay.order_service.utility.AppExtensions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("ALL")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OrderCreationResponse>> createOrder(@RequestHeader(AppExtensions.CLIENT_REQUEST_KEY_HEADER) String clientRequestKey,
                                                                          @ModelAttribute @Valid OrderCreationRequest orderCreationRequest) {

        ApiResponse<OrderCreationResponse> apiResponse = orderService.createOrder(orderCreationRequest, clientRequestKey);

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/unapproved")
    public ResponseEntity<ApiResponse<List<UnapprovedOrderResponse>>> fetchCustomerUnApprovedOrders() {

        ApiResponse<List<UnapprovedOrderResponse>> apiResponse = orderService.fetchCustomerUnApprovedOrders();

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/approve")
    public ResponseEntity<ApiResponse<String>> approveOrder(@RequestHeader(AppExtensions.CLIENT_REQUEST_KEY_HEADER) String clientRequestKey,
                                                            @Valid @RequestBody OrderApprovalRequest orderApprovalRequest) {

        ApiResponse<String> apiResponse = orderService.approveOrder(orderApprovalRequest, clientRequestKey);

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);

    }
}
