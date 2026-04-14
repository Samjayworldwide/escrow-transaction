package com.samjay.notification_service.functions.dlqs;

import com.samjay.notification_service.dtos.events.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class NotificationFunctionDlq {

    /*
      This function will be triggered when a message fails to process in the main function and is sent to the DLQ.
      You can enhance this function by saving the failed record to a database or triggering an alerting mechanism for manual intervention.
    */

    @Bean
    public Consumer<DeviceInformationEventDto> upsertDeviceInformationDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for user: {}", failedRecord.userId());

    }

    @Bean
    public Consumer<EmptyDriverSearchResultEventDto> sendNotificationToBuyerForEmptyDriverSearchResultDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for buyer user: {}", failedRecord.buyerUserId());

    }

    @Bean
    public Consumer<EmptyDriverSearchResultEventDto> sendNotificationToSellerForEmptyDriverSearchResultDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for seller user: {}", failedRecord.sellerUserId());

    }

    @Bean
    public Consumer<OrderDeliveryEventDto> sendOrderDeliveryNotificationToDriverDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for driver user: {}", failedRecord.driverUserId());
    }

    @Bean
    public Consumer<NotificationDeliveryAcceptanceEventDto> sendNotificationToBuyerForDriverAssignmentDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for buyer user: {}", failedRecord.buyerUserId());

    }

    @Bean
    public Consumer<NotificationDeliveryAcceptanceEventDto> sendNotificationToSellerForDriverAssignmentDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for seller user: {}", failedRecord.sellerUserId());

    }

    @Bean
    public Consumer<TrackingStageNotificationEventDto> sendNotificationForTrackingStageDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for user: {}", failedRecord.userId());

    }

    @Bean
    public Consumer<EscrowReleaseCompletedEventDto> sendNotificationForEscrowReleaseToBuyerDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for buyer user: {}", failedRecord.buyerUserId());

    }

    @Bean
    public Consumer<EscrowReleaseCompletedEventDto> sendNotificationForEscrowReleaseToSellerDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for seller user: {}", failedRecord.sellerUserId());

    }

    @Bean
    public Consumer<DriverWalletCreditNotificationEventDto> sendNotificationForDriverWalletCreditDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for driver user: {}", failedRecord.driverUserId());

    }

    @Bean
    public Consumer<DriverWalletCreditNotificationEventDto> sendNotificationToBuyerForDeliveryFeeDebitDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for buyer user: {}", failedRecord.buyerUserId());

    }
}

