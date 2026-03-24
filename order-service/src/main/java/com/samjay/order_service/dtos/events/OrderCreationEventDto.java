package com.samjay.order_service.dtos.events;

import com.samjay.order_service.enumerations.OrderCreator;

public record OrderCreationEventDto(String email,
                                    String orderReferenceNumber,
                                    String username,
                                    OrderCreator orderCreator) {
}
