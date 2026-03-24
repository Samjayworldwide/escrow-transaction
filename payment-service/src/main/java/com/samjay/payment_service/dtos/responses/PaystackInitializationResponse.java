package com.samjay.payment_service.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaystackInitializationResponse {

    private String accessCode;

    private String reference;

    private String authorizationUrl;
}
