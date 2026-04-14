package com.samjay.driver_service.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriverTaskResponse {

    private String orderReferenceNumber;

    private String pickupAddress;

    private String dropoffAddress;

    private BigDecimal deliveryFee;

    private LocalDateTime createdAt;

}
