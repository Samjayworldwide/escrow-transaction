package com.samjay.payment_service.controllers;

import com.samjay.payment_service.dtos.requests.PaymentRequest;
import com.samjay.payment_service.dtos.responses.ApiResponse;
import com.samjay.payment_service.dtos.responses.PaymentInitializationResponse;
import com.samjay.payment_service.services.interfaces.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.samjay.payment_service.utility.AppExtensions.CLIENT_REQUEST_KEY_HEADER;

@SuppressWarnings("NullableProblems")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<PaymentInitializationResponse>> initializePayment(@RequestHeader(CLIENT_REQUEST_KEY_HEADER) String clientRequestKey,
                                                                                        @Valid @RequestBody PaymentRequest paymentRequest) {

        ApiResponse<PaymentInitializationResponse> response = paymentService.initializePayment(clientRequestKey, paymentRequest);

        if (!response.isSuccessful())
            return ResponseEntity.badRequest().body(response);

        return ResponseEntity.ok(response);

    }

    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<String>> verifyPayment(@RequestParam("clientRequestKey") String clientRequestKey,
                                                             @RequestParam("paymentId") UUID paymentId) {

        ApiResponse<String> response = paymentService.verifyPayment(clientRequestKey, paymentId);

        if (!response.isSuccessful())
            return ResponseEntity.badRequest().body(response);

        return ResponseEntity.ok(response);
    }
}
