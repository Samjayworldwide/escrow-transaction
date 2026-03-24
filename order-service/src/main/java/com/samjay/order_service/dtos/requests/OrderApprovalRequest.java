package com.samjay.order_service.dtos.requests;

import jakarta.validation.constraints.Digits;
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
public class OrderApprovalRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Phone number is required")
    @Length(min = 11, max = 11, message = "Phone number must be exactly 11 digits long")
    @Digits(fraction = 0, integer = 11, message = "Your Phone number is incorrect!")
    private String phoneNumber;

}
