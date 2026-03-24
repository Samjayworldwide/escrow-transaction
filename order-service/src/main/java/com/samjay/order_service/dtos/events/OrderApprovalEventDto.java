package com.samjay.order_service.dtos.events;

import com.samjay.order_service.enumerations.OrderCreator;

public record OrderApprovalEventDto(String buyerEmail,
                                    String sellerEmail,
                                    String orderReferenceNumber,
                                    String buyerUsername,
                                    String sellerUsername,
                                    OrderCreator orderCreator) {
}
