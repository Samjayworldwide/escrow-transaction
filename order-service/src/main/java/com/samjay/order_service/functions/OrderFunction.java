package com.samjay.order_service.functions;

import com.samjay.order_service.dtos.events.PaymentCompletionEventDto;
import com.samjay.order_service.dtos.events.PaymentVerificationEventDto;
import com.samjay.order_service.dtos.responses.OrderDetailsDto;
import com.samjay.order_service.services.interfaces.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OrderFunction {

    private final OrderService orderService;

    @Bean
    Function<PaymentVerificationEventDto, PaymentCompletionEventDto> updateOrderAfterPaymentVerification() {

        return paymentVerificationEventDto -> {

            try {

                OrderDetailsDto orderDetailsDto = orderService.updateOrderStatusToPaid(paymentVerificationEventDto.orderId());

                return new PaymentCompletionEventDto(
                        orderDetailsDto.getBuyerEmail(),
                        orderDetailsDto.getSellerEmail(),
                        UUID.fromString(orderDetailsDto.getBuyerUserId()),
                        UUID.fromString(orderDetailsDto.getSellerUserId()),
                        orderDetailsDto.getOrderReferenceNumber(),
                        paymentVerificationEventDto.amount(),
                        paymentVerificationEventDto.paymentId(),
                        paymentVerificationEventDto.orderId(),
                        paymentVerificationEventDto.clientRequestKey()
                );

            } catch (Exception e) {

                log.error("Error updating order after payment verification: {}", e.getMessage());

                throw e;
            }
        };
    }
}
