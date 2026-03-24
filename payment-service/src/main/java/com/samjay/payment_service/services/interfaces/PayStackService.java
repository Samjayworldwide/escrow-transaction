package com.samjay.payment_service.services.interfaces;

import com.samjay.payment_service.dtos.responses.ApiResponse;
import com.samjay.payment_service.dtos.responses.PaystackInitializationResponse;

import java.math.BigDecimal;

public interface PayStackService {

    ApiResponse<PaystackInitializationResponse> paystackInitialization(String email,
                                                                       BigDecimal amount,
                                                                       String callBackUrl);

    ApiResponse<String> paystackVerifyPayment(String reference);
}
