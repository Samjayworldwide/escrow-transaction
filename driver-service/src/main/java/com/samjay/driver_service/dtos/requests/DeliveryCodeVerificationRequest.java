package com.samjay.driver_service.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryCodeVerificationRequest {

    @NotNull(message = "Order ID is required")
    UUID orderId;

    @Length(min = 6, max = 6, message = "Delivery code must be exactly 6 characters long")
    @NotBlank(message = "Delivery code is required")
    String deliveryCode;
}
