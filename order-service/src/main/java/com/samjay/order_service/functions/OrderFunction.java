package com.samjay.order_service.functions;

import com.samjay.order_service.dtos.events.PaymentVerificationEventDto;
import com.samjay.order_service.services.interfaces.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OrderFunction {

    private final OrderService orderService;

    @Bean
    public Consumer<PaymentVerificationEventDto> updateOrderAfterPaymentVerification() {

        return paymentVerificationEventDto -> {

            try {

                orderService.updateOrderStatusToPaid(paymentVerificationEventDto);

            } catch (Exception e) {

                log.error("Error updating order after payment verification: {}", e.getMessage());

                throw e;
            }
        };
    }
}
