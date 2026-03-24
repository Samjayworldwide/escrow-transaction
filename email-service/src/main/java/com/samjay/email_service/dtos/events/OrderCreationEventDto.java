package com.samjay.email_service.dtos.events;


import com.samjay.email_service.enumerations.OrderCreator;

public record OrderCreationEventDto(String email,
                                    String orderReferenceNumber,
                                    String username,
                                    OrderCreator orderCreator) {
}