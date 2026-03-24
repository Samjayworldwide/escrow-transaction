package com.samjay.payment_service.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaystackInitializationRequest {

    private String email;

    private long amount;

    private String callback_url;

    private String reference;
}
