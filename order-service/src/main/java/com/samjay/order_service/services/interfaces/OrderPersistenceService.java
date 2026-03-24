package com.samjay.order_service.services.interfaces;

import com.samjay.ValidateAndFetchCustomerUsernameResponse;
import com.samjay.order_service.dtos.requests.OrderCreationRequest;
import com.samjay.order_service.dtos.responses.ApiResponse;
import com.samjay.order_service.dtos.responses.OrderCreationResponse;
import com.samjay.order_service.dtos.responses.UserIdentifier;
import com.samjay.order_service.entities.ItemDetails;

import java.util.List;

public interface OrderPersistenceService {

    ApiResponse<OrderCreationResponse> persistOrder(OrderCreationRequest orderCreationRequest, String clientRequestKey,
                                                    UserIdentifier userIdentifier, String hashedFingerPrint,
                                                    String orderReferenceNumber, List<ItemDetails> itemDetailsList,
                                                    ValidateAndFetchCustomerUsernameResponse validateAndFetchCustomerUsernameResponse);
}
