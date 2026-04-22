package com.samjay.order_service.services.interfaces;

import com.samjay.order_service.dtos.requests.DisputeCreationRequest;
import com.samjay.order_service.dtos.responses.ApiResponse;

public interface DisputeService {

    ApiResponse<String> createDispute(DisputeCreationRequest disputeCreationRequest);

}
