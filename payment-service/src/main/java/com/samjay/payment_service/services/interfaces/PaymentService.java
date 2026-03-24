package com.samjay.payment_service.services.interfaces;

import com.samjay.payment_service.dtos.requests.PaymentRequest;
import com.samjay.payment_service.dtos.responses.ApiResponse;
import com.samjay.payment_service.dtos.responses.PaymentInitializationResponse;

import java.util.UUID;


public interface PaymentService {

    ApiResponse<PaymentInitializationResponse> initializePayment(String clientRequestKey, PaymentRequest paymentRequest);

    ApiResponse<String> verifyPayment(String clientRequestKey, UUID paymentId);
}
