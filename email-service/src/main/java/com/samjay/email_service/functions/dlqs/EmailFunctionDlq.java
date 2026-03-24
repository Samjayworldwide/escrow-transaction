package com.samjay.email_service.functions.dlqs;

import com.samjay.email_service.dtos.events.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class EmailFunctionDlq {

    /*
    This function will be triggered when a message fails to process in the main function and is sent to the DLQ.
    You can enhance this function by saving the failed record to a database or triggering an alerting mechanism for manual intervention.
     */

    @Bean
    public Consumer<EmailVerificationEventDto> sendEmailVerificationMailDlq() {

        return failedRecord -> log.error(
                "Message landed in DLQ — manual intervention required: {}",
                failedRecord.email()
        );
    }

    @Bean
    public Consumer<OrderCreationEventDto> sendOrderCreationMailDlq() {

        return failedRecord -> log.error(
                "Message landed in DLQ — manual intervention required: {}",
                failedRecord.orderReferenceNumber()
        );
    }

    @Bean
    public Consumer<OrderApprovalEventDto> sendBuyerOrderApprovalMailDlq() {

        return failedRecord -> log.error(
                "Message landed in DLQ — manual intervention required: {}",
                failedRecord.orderReferenceNumber()
        );
    }

    @Bean
    public Consumer<OrderApprovalEventDto> sendSellerOrderApprovalMailDlq() {

        return failedRecord -> log.error(
                "Message landed in DLQ — manual intervention required: {}",
                failedRecord.orderReferenceNumber()
        );
    }

    @Bean
    public Consumer<PaymentInitializationEventDto> sendPaymentInitializationMailDlq() {

        return failedRecord -> log.error(
                "Message landed in DLQ — manual intervention required: {}",
                failedRecord.orderReferenceNumber()
        );
    }

    @Bean
    public Consumer<PaymentCompletionEventDto> sendBuyerPaymentCompletionMailDlq() {

        return failedRecord -> log.error(
                "Message landed in DLQ — manual intervention required: {}",
                failedRecord.orderReferenceNumber()
        );
    }

    @Bean
    public Consumer<PaymentCompletionEventDto> sendSellerPaymentCompletionMailDlq() {

        return failedRecord -> log.error(
                "Message landed in DLQ — manual intervention required: {}",
                failedRecord.orderReferenceNumber()
        );
    }
}
