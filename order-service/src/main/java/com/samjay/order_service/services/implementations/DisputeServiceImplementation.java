package com.samjay.order_service.services.implementations;

import com.samjay.order_service.configurations.AuthenticatedUserProvider;
import com.samjay.order_service.dtos.requests.DisputeCreationRequest;
import com.samjay.order_service.dtos.responses.ApiResponse;
import com.samjay.order_service.dtos.responses.UserIdentifier;
import com.samjay.order_service.entities.Dispute;
import com.samjay.order_service.entities.Order;
import com.samjay.order_service.enumerations.OrderStatus;
import com.samjay.order_service.repositories.DisputeRepository;
import com.samjay.order_service.repositories.OrderRepository;
import com.samjay.order_service.services.interfaces.DisputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeServiceImplementation implements DisputeService {

    private final DisputeRepository disputeRepository;

    private final OrderRepository orderRepository;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    @McpTool(name = "create-dispute", description = "Create a dispute for an order that has not been settled")
    @Transactional
    @Override
    public ApiResponse<String> createDispute(@McpToolParam(description = """
            The dispute creation details. All fields are required:
            - orderReferenceNumber: the unique reference number of the order being disputed
            - disputeDescription: a detailed explanation of the dispute reason
            - disputeReason: one of the enum values representing the category of dispute
            """) DisputeCreationRequest disputeCreationRequest) {

        try {

            if (disputeCreationRequest.getOrderReferenceNumber() == null || disputeCreationRequest.getOrderReferenceNumber().isBlank())
                return ApiResponse.error("Order reference number is required");

            if (disputeCreationRequest.getDisputeDescription() == null || disputeCreationRequest.getDisputeDescription().isBlank())
                return ApiResponse.error("Dispute description is required");

            if (disputeCreationRequest.getDisputeReason() == null)
                return ApiResponse.error("Dispute reason is required");

            UserIdentifier userIdentifier = authenticatedUserProvider.getCurrentLoggedInUser();

            Optional<Order> optionalOrder = orderRepository.findByOrderReferenceNumber(disputeCreationRequest.getOrderReferenceNumber());

            if (optionalOrder.isEmpty())
                return ApiResponse.error("Order not found");

            Order order = optionalOrder.get();

            if (order.getOrderStatus() != OrderStatus.DELIVERED)
                return ApiResponse.error("Disputes can only be created for orders that have been delivered");

            boolean disputeExists = disputeRepository.existsByOrderIdAndCreatorUserId(order.getId(), UUID.fromString(userIdentifier.userId()));

            if (disputeExists)
                return ApiResponse.error("You have already created a dispute for this order");

            Dispute dispute = new Dispute();

            dispute.setOrderId(order.getId());

            dispute.setOrderReferenceNumber(disputeCreationRequest.getOrderReferenceNumber());

            dispute.setCreatorUserId(UUID.fromString(userIdentifier.userId()));

            dispute.setDisputeReason(disputeCreationRequest.getDisputeReason());

            dispute.setDisputeDescription(disputeCreationRequest.getDisputeDescription());

            order.setOrderStatus(OrderStatus.DISPUTED);

            disputeRepository.save(dispute);

            orderRepository.save(order);

            return ApiResponse.success("Dispute created successfully");

        } catch (Exception e) {

            log.error("An error occurred while creating dispute: ", e);

            throw new RuntimeException("Error creating dispute: " + e.getMessage());

        }
    }
}
