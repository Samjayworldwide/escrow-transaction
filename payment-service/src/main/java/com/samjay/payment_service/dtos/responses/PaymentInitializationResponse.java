package com.samjay.payment_service.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInitializationResponse {

    private UUID paymentId;

    private String accessCode;

    private String authorizationUrl;
}
