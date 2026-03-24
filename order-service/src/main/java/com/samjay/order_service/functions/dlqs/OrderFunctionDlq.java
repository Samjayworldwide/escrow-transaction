package com.samjay.order_service.functions.dlqs;

import com.samjay.order_service.dtos.events.PaymentCompletionEventDto;
import com.samjay.order_service.dtos.events.PaymentVerificationEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
@Slf4j
public class OrderFunctionDlq {

    @Bean
    public Function<PaymentVerificationEventDto, PaymentCompletionEventDto> updateOrderAfterPaymentVerificationDlq() {

        return paymentVerificationEventDto -> {

            log.error("Received message in DLQ for order ID: {}", paymentVerificationEventDto.orderId());

            log.error("Message details: {}", paymentVerificationEventDto);

            // Here you can implement any additional logic for handling the failed message,
            // such as sending notifications, alerting, or storing the message for further analysis.

            return null; // Return null or an appropriate response based on your requirements.
        };
    }
}
