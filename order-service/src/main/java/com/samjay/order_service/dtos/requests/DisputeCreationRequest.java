package com.samjay.order_service.dtos.requests;

import com.samjay.order_service.enumerations.DisputeReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DisputeCreationRequest {

    @NotBlank(message = "Order reference number is required")
    private String orderReferenceNumber;

    @NotBlank(message = "Dispute description is required")
    private String disputeDescription;

    @NotNull(message = "Dispute reason is required")
    private DisputeReason disputeReason;
}
