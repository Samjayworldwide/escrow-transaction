package com.samjay.order_service.dtos.requests;

import com.samjay.order_service.enumerations.OrderCreator;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreationRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Phone number is required")
    @Length(min = 11, max = 11, message = "Phone number must be exactly 11 digits long")
    @Digits(fraction = 0, integer = 11, message = "Your Phone number is incorrect!")
    private String phoneNumber;

    @NotNull(message = "Order creator is required")
    private OrderCreator orderCreator;

    @NotEmpty(message = "Item details list cannot be empty")
    private List<ItemDetailsRequest> itemDetails;

}
