package com.samjay.order_service.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailsDto {

    private String orderReferenceNumber;

    private String buyerEmail;

    private String buyerUserId;

    private String sellerEmail;

    private String sellerUserId;
}
