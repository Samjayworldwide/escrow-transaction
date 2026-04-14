package com.samjay.wallet_service.dtos.events;

import java.util.UUID;

public record OrderSettlementEventDto(UUID orderId,
                                      UUID buyerUserId,
                                      UUID sellerUserId,
                                      String buyerEmail,
                                      String sellerEmail,
                                      String orderReferenceNumber,
                                      String clientRequestKey
) {
}
