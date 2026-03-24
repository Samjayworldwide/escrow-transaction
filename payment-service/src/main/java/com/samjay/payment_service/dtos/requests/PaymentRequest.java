package com.samjay.payment_service.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Amount cannot be empty")
    private BigDecimal amount;

    @NotBlank(message = "Description cannot be empty")
    private String description;

    @NotNull(message = "Order ID cannot be empty")
    private UUID orderId;
}
