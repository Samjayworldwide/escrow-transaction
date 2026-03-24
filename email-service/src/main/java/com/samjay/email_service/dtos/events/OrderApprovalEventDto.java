package com.samjay.email_service.dtos.events;


import com.samjay.email_service.enumerations.OrderCreator;

public record OrderApprovalEventDto(String buyerEmail,
                                    String sellerEmail,
                                    String orderReferenceNumber,
                                    String buyerUsername,
                                    String sellerUsername,
                                    OrderCreator orderCreator) {
}
