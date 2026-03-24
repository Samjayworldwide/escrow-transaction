package com.samjay.order_service.dtos.responses;

import com.samjay.order_service.enumerations.OrderStatus;
import com.samjay.order_service.enumerations.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnapprovedOrderResponse {

    private UUID orderId;

    private String orderReferenceNumber;

    private OrderStatus orderStatus;

    private PaymentStatus paymentStatus;

    private String creatorUsername;

    private LocalDateTime createdAt;

    private List<ItemDetailsResponse> itemDetails;
}
