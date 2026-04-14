package com.samjay.notification_service.functions;

import com.samjay.notification_service.dtos.events.*;
import com.samjay.notification_service.dtos.requests.DeviceUpsertRequest;
import com.samjay.notification_service.services.interfaces.DeviceService;
import com.samjay.notification_service.services.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.function.Consumer;

import static com.samjay.notification_service.utilities.AppExtensions.generateHash;
import static com.samjay.notification_service.utilities.AppExtensions.serialize;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class NotificationFunction {

    private final DeviceService deviceService;

    private final NotificationService notificationService;

    @Bean
    public Consumer<DeviceInformationEventDto> upsertDeviceInformation() {

        return deviceInformationEventDto -> {

            if (deviceInformationEventDto == null)
                return;

            try {

                log.info("Received User login event to upsert device information for user with user ID: {}", deviceInformationEventDto.userId());

                DeviceUpsertRequest deviceUpsertRequest = new DeviceUpsertRequest(
                        deviceInformationEventDto.userId(),
                        deviceInformationEventDto.deviceImei(),
                        deviceInformationEventDto.firebaseToken(),
                        deviceInformationEventDto.deviceModel(),
                        deviceInformationEventDto.osVersion(),
                        deviceInformationEventDto.devicePlatform()
                );

                deviceService.upsertDevice(deviceUpsertRequest);

                log.info("Sucessfully upserted device information for user with userId: {}", deviceInformationEventDto.userId());

            } catch (Exception e) {

                log.error("Error processing UserLoginEventDto: {}. Exception: {}", deviceInformationEventDto, e.getMessage(), e);

                throw e;

            }
        };
    }

    @Bean
    public Consumer<EmptyDriverSearchResultEventDto> sendNotificationToBuyerForEmptyDriverSearchResult() {

        return emptyDriverSearchResultEventDto -> {

            if (emptyDriverSearchResultEventDto == null) {

                log.warn("Received null EmptyDriverSearchResultEventDto. Skipping notification.");

                return;

            }

            try {

                log.info("About sending notification for empty driver search result for user with user ID: {}", emptyDriverSearchResultEventDto.buyerUserId());

                String title = "No drivers found";

                String message = "Sorry, we couldn't find any drivers nearby. Please try again logging into your application to continue the search.";

                String rawKey = serialize(emptyDriverSearchResultEventDto) +
                        title +
                        message +
                        emptyDriverSearchResultEventDto.buyerUserId().toString();

                String idempotencyKey = generateHash(rawKey);

                notificationService.sendAndSavePushNotification(
                        emptyDriverSearchResultEventDto.buyerUserId(),
                        title,
                        message,
                        idempotencyKey
                );

                log.info("Successfully sent notification for empty driver search result for user with user ID: {}", emptyDriverSearchResultEventDto.buyerUserId());

            } catch (Exception e) {

                log.error("Error sending notification to user with userId {} for empty driver search. Exception: {}", emptyDriverSearchResultEventDto.buyerUserId(), e.getMessage(), e);

                throw e;

            }
        };
    }

    @Bean
    public Consumer<EmptyDriverSearchResultEventDto> sendNotificationToSellerForEmptyDriverSearchResult() {

        return emptyDriverSearchResultEventDto -> {

            if (emptyDriverSearchResultEventDto == null) {

                log.warn("Received null EmptyDriverSearchResultEventDto. Skipping notification.");

                return;

            }

            try {

                log.info("About sending notification for empty driver search result for user with user ID: {}", emptyDriverSearchResultEventDto.sellerUserId());

                String title = "No drivers found";

                String message = "Sorry, we couldn't find any drivers nearby. We are still on the search for a driver.";

                String rawKey = serialize(emptyDriverSearchResultEventDto) +
                        title +
                        message +
                        emptyDriverSearchResultEventDto.sellerUserId().toString();

                String idempotencyKey = generateHash(rawKey);

                notificationService.sendAndSavePushNotification(
                        emptyDriverSearchResultEventDto.sellerUserId(),
                        title,
                        message,
                        idempotencyKey
                );

                log.info("Successfully sent notification for empty driver search result for user with user ID: {}", emptyDriverSearchResultEventDto.sellerUserId());

            } catch (Exception e) {

                log.error("Error sending notification to user with userId {} for empty driver search. Exception: {}", emptyDriverSearchResultEventDto.sellerUserId(), e.getMessage(), e);

                throw e;

            }
        };
    }

    @Bean
    public Consumer<OrderDeliveryEventDto> sendOrderDeliveryNotificationToDriver() {

        return orderDeliveryEventDto -> {

            if (orderDeliveryEventDto == null) {

                log.warn("Received null OrderDeliveryEventDto. Skipping notification.");

                return;

            }

            try {

                log.info("About sending order delivery notification to driver with user ID: {}", orderDeliveryEventDto.driverUserId());

                String rawKey = serialize(orderDeliveryEventDto) +
                        orderDeliveryEventDto.notificationTitle() +
                        orderDeliveryEventDto.notificationBody() +
                        orderDeliveryEventDto.driverUserId().toString();

                String idempotencyKey = generateHash(rawKey);

                notificationService.sendAndSavePushNotification(
                        orderDeliveryEventDto.driverUserId(),
                        orderDeliveryEventDto.notificationTitle(),
                        orderDeliveryEventDto.notificationBody(),
                        idempotencyKey
                );

                log.info("Successfully sent order delivery notification to driver with user ID: {}", orderDeliveryEventDto.driverUserId());

            } catch (Exception e) {

                log.error("Error sending order delivery notification to driver with userId {}. Exception: {}", orderDeliveryEventDto.driverUserId(), e.getMessage(), e);

                throw e;

            }
        };
    }

    @Bean
    public Consumer<NotificationDeliveryAcceptanceEventDto> sendNotificationToBuyerForDriverAssignment() {

        return notificationDeliveryAcceptanceEventDto -> {

            if (notificationDeliveryAcceptanceEventDto == null) {

                log.warn("Received null DriverAssignmentNotificationEventDto. Skipping notification.");

                return;

            }

            try {

                log.info("About sending driver assignment notification to buyer with user ID: {}", notificationDeliveryAcceptanceEventDto.buyerUserId());

                String title = "Driver Assigned";

                String message = String.format("Good news! A driver has been assigned to your order %s. Driver full name: %s %s, Vehicle: %s, Contact: %s " +
                                "please provide the delivery code %s to the driver when the items arrive your address. Thank you for using our service!",
                        notificationDeliveryAcceptanceEventDto.orderReferenceNumber(),
                        notificationDeliveryAcceptanceEventDto.driverFirstname(),
                        notificationDeliveryAcceptanceEventDto.driverLastname(),
                        notificationDeliveryAcceptanceEventDto.vehicleLicenseNumber(),
                        notificationDeliveryAcceptanceEventDto.driverPhoneNumber(),
                        notificationDeliveryAcceptanceEventDto.dropoffDeliveryCode()
                );

                String rawKey = serialize(notificationDeliveryAcceptanceEventDto) +
                        title +
                        message +
                        notificationDeliveryAcceptanceEventDto.buyerUserId().toString();

                String idempotencyKey = generateHash(rawKey);

                notificationService.sendAndSavePushNotification(
                        notificationDeliveryAcceptanceEventDto.buyerUserId(),
                        title,
                        message,
                        idempotencyKey
                );

                log.info("Successfully sent driver assignment notification to buyer with user ID: {}", notificationDeliveryAcceptanceEventDto.buyerUserId());

            } catch (Exception e) {

                log.error("Error sending driver assignment notification to buyer with userId {}. Exception: {}", notificationDeliveryAcceptanceEventDto.buyerUserId(), e.getMessage(), e);

                throw e;

            }
        };
    }

    @Bean
    public Consumer<NotificationDeliveryAcceptanceEventDto> sendNotificationToSellerForDriverAssignment() {

        return notificationDeliveryAcceptanceEventDto -> {

            if (notificationDeliveryAcceptanceEventDto == null) {

                log.warn("Received null DriverAssignmentNotificationEventDto. Skipping notification.");

                return;

            }

            try {

                log.info("About sending driver assignment notification to seller with user ID: {}", notificationDeliveryAcceptanceEventDto.sellerUserId());

                String title = "Driver Assigned";

                String message = String.format("Good news! A driver has been assigned to your order %s. Driver full name: %s %s, Vehicle: %s, Contact: %s " +
                                "please provide the delivery code %s to the driver when the items are picked up from your address. Thank you for using our service!",
                        notificationDeliveryAcceptanceEventDto.orderReferenceNumber(),
                        notificationDeliveryAcceptanceEventDto.driverFirstname(),
                        notificationDeliveryAcceptanceEventDto.driverLastname(),
                        notificationDeliveryAcceptanceEventDto.vehicleLicenseNumber(),
                        notificationDeliveryAcceptanceEventDto.driverPhoneNumber(),
                        notificationDeliveryAcceptanceEventDto.pickupDeliveryCode()
                );

                String rawKey = serialize(notificationDeliveryAcceptanceEventDto) +
                        title +
                        message +
                        notificationDeliveryAcceptanceEventDto.sellerUserId().toString();

                String idempotencyKey = generateHash(rawKey);

                notificationService.sendAndSavePushNotification(
                        notificationDeliveryAcceptanceEventDto.sellerUserId(),
                        title,
                        message,
                        idempotencyKey
                );

                log.info("Successfully sent driver assignment notification to seller with user ID: {}", notificationDeliveryAcceptanceEventDto.sellerUserId());

            } catch (Exception e) {

                log.error("Error sending driver assignment notification to seller with userId {}. Exception: {}", notificationDeliveryAcceptanceEventDto.sellerUserId(), e.getMessage(), e);

                throw e;

            }
        };
    }

    @Bean
    public Consumer<TrackingStageNotificationEventDto> sendNotificationForTrackingStage() {

        return trackingStageNotificationEventDto -> {

            try {

                if (trackingStageNotificationEventDto == null) {

                    log.warn("Received null TrackingStageNotificationEventDto. Skipping notification.");

                    return;
                }

                String title = "Order Update";

                String notificationMessage = trackingStageNotificationEventDto.isBuyer() ? String.format(
                        "The order with reference number %s has be received from the seller and its on the way to your provided address",
                        trackingStageNotificationEventDto.orderReferenceNumber()
                ) : String.format(
                        "The order with reference number %s has reached the buyer's address, if there is no dispute within the next 30 minutes, the order will be marked as completed.",
                        trackingStageNotificationEventDto.orderReferenceNumber()
                );

                String rawKey = serialize(trackingStageNotificationEventDto) +
                        title +
                        notificationMessage +
                        trackingStageNotificationEventDto.userId();

                String idempotencyKey = generateHash(rawKey);

                notificationService.sendAndSavePushNotification(
                        UUID.fromString(trackingStageNotificationEventDto.userId()),
                        title,
                        notificationMessage,
                        idempotencyKey
                );

                log.info("Successfully sent tracking stage notification to user with user ID: {}", trackingStageNotificationEventDto.userId());

            } catch (Exception ex) {

                log.error("Error creating sendNotificationForTrackingStage consumer function. Exception: {}", ex.getMessage(), ex);

                throw ex;

            }
        };
    }

    @Bean
    public Consumer<EscrowReleaseCompletedEventDto> sendNotificationForEscrowReleaseToBuyer() {

        return escrowReleaseCompletedEventDto -> {

            try {

                if (escrowReleaseCompletedEventDto == null) {

                    log.warn("Received null EscrowReleaseCompletedEventDto. Skipping notification.");

                    return;

                }

                String title = "Order Completed";

                String message = String.format("The order with reference number %s has been marked as completed and the amount of ₦%s has been debited from your wallet and released from escrow to the seller, Your available balance is now ₦%s. Thank you for using our service!",
                        escrowReleaseCompletedEventDto.orderReferenceNumber(),
                        escrowReleaseCompletedEventDto.amount().toPlainString(),
                        escrowReleaseCompletedEventDto.buyerAvailableBalance().toPlainString()
                );

                String rawKey = serialize(escrowReleaseCompletedEventDto) +
                        title +
                        message +
                        escrowReleaseCompletedEventDto.buyerUserId().toString();

                String idempotencyKey = generateHash(rawKey);

                notificationService.sendAndSavePushNotification(
                        escrowReleaseCompletedEventDto.buyerUserId(),
                        title,
                        message,
                        idempotencyKey
                );

                log.info("Successfully sent escrow release notification to buyer with user ID: {}", escrowReleaseCompletedEventDto.buyerUserId());

            } catch (Exception ex) {

                log.error("Error creating sendNotificationForEscrowReleaseToBuyer consumer function. Exception: {}", ex.getMessage(), ex);

                throw ex;

            }
        };
    }

    @Bean
    public Consumer<EscrowReleaseCompletedEventDto> sendNotificationForEscrowReleaseToSeller() {

        return escrowReleaseCompletedEventDto -> {

            try {

                if (escrowReleaseCompletedEventDto == null) {

                    log.warn("Received null EscrowReleaseCompletedEventDto. Skipping notification.");

                    return;

                }

                String title = "Order Completed";

                String message = String.format("The order with reference number %s has been marked as completed and the amount of ₦%s has been credited to your wallet from escrow, Your available balance is now ₦%s. Thank you for using our service!",
                        escrowReleaseCompletedEventDto.orderReferenceNumber(),
                        escrowReleaseCompletedEventDto.amount().toPlainString(),
                        escrowReleaseCompletedEventDto.sellerAvailableBalance().toPlainString()
                );

                String rawKey = serialize(escrowReleaseCompletedEventDto) +
                        title +
                        message +
                        escrowReleaseCompletedEventDto.sellerUserId().toString();

                String idempotencyKey = generateHash(rawKey);

                notificationService.sendAndSavePushNotification(
                        escrowReleaseCompletedEventDto.sellerUserId(),
                        title,
                        message,
                        idempotencyKey
                );

                log.info("Successfully sent escrow release notification to seller with user ID: {}", escrowReleaseCompletedEventDto.sellerUserId());

            } catch (Exception ex) {

                log.error("Error creating sendNotificationForEscrowReleaseToSeller consumer function. Exception: {}", ex.getMessage(), ex);

                throw ex;

            }
        };
    }

    @Bean
    public Consumer<DriverWalletCreditNotificationEventDto> sendNotificationForDriverWalletCredit() {

        return driverWalletCreditNotificationEventDto -> {

            try {

                if (driverWalletCreditNotificationEventDto == null) {

                    log.warn("Received null DriverWalletCreditNotification. Skipping notification.");

                    return;

                }

                String title = "Wallet Credited";

                String message = String.format("Your wallet has been credited with the amount of ₦%s for order with reference number %s. Your available balance is now ₦%s",
                        driverWalletCreditNotificationEventDto.amount().toPlainString(),
                        driverWalletCreditNotificationEventDto.orderReferenceNumber(),
                        driverWalletCreditNotificationEventDto.driverAvailableBalance().toPlainString()
                );

                String rawKey = serialize(driverWalletCreditNotificationEventDto) +
                        title +
                        message +
                        driverWalletCreditNotificationEventDto.driverUserId().toString();

                String idempotencyKey = generateHash(rawKey);

                notificationService.sendAndSavePushNotification(
                        driverWalletCreditNotificationEventDto.driverUserId(),
                        title,
                        message,
                        idempotencyKey
                );

                log.info("Successfully sent wallet credit notification to driver with user ID: {}", driverWalletCreditNotificationEventDto.driverUserId());

            } catch (Exception ex) {

                log.error("Error creating sendNotificationForDriverWalletCredit consumer function. Exception: {}", ex.getMessage(), ex);

                throw ex;

            }
        };
    }

    @Bean
    public Consumer<DriverWalletCreditNotificationEventDto> sendNotificationToBuyerForDeliveryFeeDebit() {

        return driverWalletCreditNotificationEventDto -> {

            try {

                if (driverWalletCreditNotificationEventDto == null) {

                    log.warn("Received null DriverWalletCreditNotification. Skipping notification.");

                    return;

                }

                String title = "Delivery Fee Debited";

                String message = String.format("Your wallet has been debited with the delivery fee amount of ₦%s for order with reference number %s. Your locked balance is now ₦%s",
                        driverWalletCreditNotificationEventDto.amount().toPlainString(),
                        driverWalletCreditNotificationEventDto.orderReferenceNumber(),
                        driverWalletCreditNotificationEventDto.buyerLockedBalance().toPlainString()
                );

                String rawKey = serialize(driverWalletCreditNotificationEventDto) +
                        title +
                        message +
                        driverWalletCreditNotificationEventDto.buyerUserId().toString();

                String idempotencyKey = generateHash(rawKey);

                notificationService.sendAndSavePushNotification(
                        driverWalletCreditNotificationEventDto.buyerUserId(),
                        title,
                        message,
                        idempotencyKey
                );

                log.info("Successfully sent delivery fee debit notification to buyer with user ID: {}", driverWalletCreditNotificationEventDto.buyerUserId());

            } catch (Exception ex) {

                log.error("Error creating sendNotificationToBuyerForDeliveryFeeDebit consumer function. Exception: {}", ex.getMessage(), ex);

                throw ex;

            }
        };
    }
}
