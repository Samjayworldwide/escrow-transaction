package com.samjay.order_service.functions.dlqs;

import com.samjay.order_service.dtos.events.OrderDeliveryUpdateEventDto;
import com.samjay.order_service.dtos.events.OrderTrackingStageUpdateEventDto;
import com.samjay.order_service.dtos.events.PaymentVerificationEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class OrderFunctionDlq {

    // Here you can implement any additional logic for handling the failed message,
    // such as sending notifications, alerting, or storing the message for further analysis.

    @Bean
    public Consumer<PaymentVerificationEventDto> updateOrderAfterPaymentVerificationDlq() {

        return paymentVerificationEventDto -> log.error(
                "Received message in DLQ for order ID: {}",
                paymentVerificationEventDto.orderId()
        );

    }

    @Bean
    public Consumer<OrderDeliveryUpdateEventDto> updateOrderAfterAssignedToDriverDlq() {

        return orderDeliveryUpdateEventDto -> log.error(
                "Received message in DLQ for order ID: {}", orderDeliveryUpdateEventDto.orderId()
        );

    }

    @Bean
    public Consumer<OrderTrackingStageUpdateEventDto> updateOrderTrackingStageDlq() {

        return orderTrackingStageUpdateEventDto -> log.error(
                "Received message in DLQ for order ID: {}",
                orderTrackingStageUpdateEventDto.orderId()
        );

    }
}
