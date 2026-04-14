package com.samjay.email_service.functions;

import com.samjay.email_service.dtos.events.*;
import com.samjay.email_service.dtos.requests.EmailRequestDto;
import com.samjay.email_service.services.interfaces.EmailService;
import com.samjay.email_service.utils.AppExtensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

import static com.samjay.email_service.utils.AppExtensions.ORDER_APPROVAL_SUBJECT;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EmailFunction {

    private final EmailService emailService;

    @Bean
    public Consumer<EmailVerificationEventDto> sendEmailVerificationMail() {

        return emailVerificationEventDto -> {

            try {

                String mailBody = AppExtensions.getVerificationMailBody(emailVerificationEventDto.verificationCode());

                EmailRequestDto emailRequestDto = EmailRequestDto
                        .builder()
                        .recipient(emailVerificationEventDto.email())
                        .subject("Email Verification")
                        .messageBody(mailBody)
                        .build();

                log.info("Sending email verification to: {}", emailVerificationEventDto.email());

                emailService.sendEmail(emailRequestDto);

                log.info("Email verification sent to: {}", emailVerificationEventDto.email());

            } catch (Exception ex) {

                log.error("Failed to process email verification for: {} — {}", emailVerificationEventDto.email(), ex.getMessage(), ex);

                throw ex;
            }
        };
    }

    @Bean
    public Consumer<OrderCreationEventDto> sendOrderCreationMail() {

        return orderCreationEventDto -> {

            String mailBody = AppExtensions.getOrderCreationMailBody(
                    orderCreationEventDto.orderCreator(),
                    orderCreationEventDto.username(),
                    orderCreationEventDto.orderReferenceNumber()
            );

            try {

                EmailRequestDto emailRequestDto = EmailRequestDto
                        .builder()
                        .messageBody(mailBody)
                        .subject("Order Creation Notification")
                        .recipient(orderCreationEventDto.email())
                        .build();

                log.info("Sending order creation email to: {}", orderCreationEventDto.email());

                emailService.sendEmail(emailRequestDto);

                log.info("Order creation email sent to: {}", orderCreationEventDto.email());

            } catch (Exception ex) {

                log.error("Failed to process order creation email for: {} — {}", orderCreationEventDto.email(), ex.getMessage(), ex);

                throw ex;
            }
        };
    }

    @Bean
    public Consumer<OrderApprovalEventDto> sendBuyerOrderApprovalMail() {

        return orderApprovalEventDto -> {

            String buyerMailBody = AppExtensions.getOrderApprovalByBuyerMailBody(
                    orderApprovalEventDto.sellerUsername(),
                    orderApprovalEventDto.orderReferenceNumber(),
                    orderApprovalEventDto.orderCreator()
            );

            try {

                EmailRequestDto buyerEmailRequestDto = EmailRequestDto
                        .builder()
                        .messageBody(buyerMailBody)
                        .subject(ORDER_APPROVAL_SUBJECT)
                        .recipient(orderApprovalEventDto.buyerEmail())
                        .build();

                log.info("Sending order approval email to buyer: {} for order reference number: {}",
                        orderApprovalEventDto.buyerEmail(), orderApprovalEventDto.orderReferenceNumber());

                emailService.sendEmail(buyerEmailRequestDto);

                log.info("Order approval emails sent to buyer: {} for order reference number: {}",
                        orderApprovalEventDto.buyerEmail(), orderApprovalEventDto.orderReferenceNumber());

            } catch (Exception ex) {

                log.error("Failed to process order approval email for buyer with email: {} and seller with email: {} — {}",
                        orderApprovalEventDto.buyerEmail(), orderApprovalEventDto.sellerEmail(), ex.getMessage(), ex);

                throw ex;
            }
        };
    }

    @Bean
    public Consumer<OrderApprovalEventDto> sendSellerOrderApprovalMail() {

        return orderApprovalEventDto -> {

            String sellerMailBody = AppExtensions.getOrderApprovalBySellerMailBody(
                    orderApprovalEventDto.buyerUsername(),
                    orderApprovalEventDto.orderReferenceNumber(),
                    orderApprovalEventDto.orderCreator()
            );

            try {

                EmailRequestDto emailRequestDto = EmailRequestDto
                        .builder()
                        .messageBody(sellerMailBody)
                        .subject(ORDER_APPROVAL_SUBJECT)
                        .recipient(orderApprovalEventDto.sellerEmail())
                        .build();

                log.info("Sending order approval email to seller: {} for order reference number: {}",
                        orderApprovalEventDto.sellerEmail(), orderApprovalEventDto.orderReferenceNumber());

                emailService.sendEmail(emailRequestDto);

                log.info("Order approval email sent to seller: {} for order reference number: {}",
                        orderApprovalEventDto.sellerEmail(), orderApprovalEventDto.orderReferenceNumber());

            } catch (Exception ex) {

                log.error("Failed to process order approval email for seller with email: {} — {}",
                        orderApprovalEventDto.sellerEmail(), ex.getMessage(), ex);

                throw ex;
            }
        };
    }

    @Bean
    public Consumer<PaymentInitializationEventDto> sendPaymentInitializationMail() {

        return paymentInitializationEventDto -> {

            try {

                String mailBody = AppExtensions.getPaymentInitiationMailBody(
                        paymentInitializationEventDto.orderReferenceNumber(),
                        paymentInitializationEventDto.authorizationUrl()
                );

                EmailRequestDto emailRequestDto = EmailRequestDto
                        .builder()
                        .recipient(paymentInitializationEventDto.email())
                        .subject("Payment Initialization")
                        .messageBody(mailBody)
                        .build();

                log.info("Sending payment verification email to: {} for order reference number: {}",
                        paymentInitializationEventDto.email(),
                        paymentInitializationEventDto.orderReferenceNumber()
                );

                emailService.sendEmail(emailRequestDto);

                log.info("Payment verification email sent to: {} for order reference number: {}",
                        paymentInitializationEventDto.email(),
                        paymentInitializationEventDto.orderReferenceNumber()
                );

            } catch (Exception e) {

                log.error("Failed to process payment verification email for: {} — {}", paymentInitializationEventDto.email(), e.getMessage(), e);

                throw e;
            }
        };
    }

    @Bean
    public Consumer<PaymentCompletionEventDto> sendBuyerPaymentCompletionMail() {

        return paymentCompletionRecordDto -> {

            try {

                String mailBody = AppExtensions.getPaymentCompletionMailBody(
                        paymentCompletionRecordDto.orderReferenceNumber(),
                        paymentCompletionRecordDto.amount().toPlainString(),
                        true
                );

                EmailRequestDto buyerEmailRequestDto = EmailRequestDto
                        .builder()
                        .recipient(paymentCompletionRecordDto.buyerEmail())
                        .subject("Payment Completed")
                        .messageBody(mailBody)
                        .build();

                log.info("Sending payment confirmation email to: {} for order reference number: {}",
                        paymentCompletionRecordDto.buyerEmail(),
                        paymentCompletionRecordDto.orderReferenceNumber()
                );

                emailService.sendEmail(buyerEmailRequestDto);

                log.info("Payment confirmation email sent to: {} for order reference number: {}",
                        paymentCompletionRecordDto.buyerEmail(),
                        paymentCompletionRecordDto.orderReferenceNumber()
                );

            } catch (Exception e) {

                log.error("Failed to process payment confirmation email for: {} — {}", paymentCompletionRecordDto.buyerEmail(), e.getMessage(), e);

                throw e;
            }
        };
    }

    @Bean
    public Consumer<PaymentCompletionEventDto> sendSellerPaymentCompletionMail() {

        return paymentCompletionRecordDto -> {

            try {

                String mailBody = AppExtensions.getPaymentCompletionMailBody(
                        paymentCompletionRecordDto.orderReferenceNumber(),
                        paymentCompletionRecordDto.amount().toPlainString(),
                        false
                );

                EmailRequestDto emailRequestDto = EmailRequestDto
                        .builder()
                        .recipient(paymentCompletionRecordDto.sellerEmail())
                        .subject("Payment Completed")
                        .messageBody(mailBody)
                        .build();

                log.info("Sending payment confirmation email to: {} for order reference number: {}",
                        paymentCompletionRecordDto.sellerEmail(),
                        paymentCompletionRecordDto.orderReferenceNumber()
                );

                emailService.sendEmail(emailRequestDto);

                log.info("Payment confirmation email sent to: {} for order reference number: {}",
                        paymentCompletionRecordDto.sellerEmail(),
                        paymentCompletionRecordDto.orderReferenceNumber()
                );

            } catch (Exception e) {

                log.error("Failed to process payment confirmation email for: {} — {}", paymentCompletionRecordDto.sellerEmail(), e.getMessage(), e);

                throw e;
            }
        };
    }

    @Bean
    public Consumer<EscrowReleaseCompletedEventDto> sendEscrowReleaseMailToBuyer() {

        return escrowReleaseCompletedEventDto -> {

            try {

                String mailBody = AppExtensions.getEscrowReleaseMailBody(
                        escrowReleaseCompletedEventDto.orderReferenceNumber(),
                        escrowReleaseCompletedEventDto.amount().toPlainString(),
                        escrowReleaseCompletedEventDto.buyerAvailableBalance().toPlainString(),
                        escrowReleaseCompletedEventDto.sellerAvailableBalance().toPlainString(),
                        true
                );

                EmailRequestDto buyerEmailRequestDto = EmailRequestDto
                        .builder()
                        .recipient(escrowReleaseCompletedEventDto.buyerEmail())
                        .subject("Escrow Release Completed. You have been debited")
                        .messageBody(mailBody)
                        .build();

                log.info("Sending escrow release confirmation email to: {} for order reference number: {}",
                        escrowReleaseCompletedEventDto.buyerEmail(),
                        escrowReleaseCompletedEventDto.orderReferenceNumber()
                );

                emailService.sendEmail(buyerEmailRequestDto);

                log.info("Escrow release confirmation email sent to: {} for order reference number: {}",
                        escrowReleaseCompletedEventDto.buyerEmail(),
                        escrowReleaseCompletedEventDto.orderReferenceNumber()
                );

            } catch (Exception e) {

                log.error("Failed to process escrow release confirmation email for: {} — {}", escrowReleaseCompletedEventDto.buyerEmail(), e.getMessage(), e);

                throw e;
            }
        };
    }

    @Bean
    public Consumer<EmailDeliveryAcceptanceEventDto> sendMailToBuyerAfterDriverAcceptsDelivery() {

        return emailDeliveryAcceptanceEventDto -> {

            try {

                String mailBody = AppExtensions.getEmailDeliveryAcceptanceMailBody(
                        emailDeliveryAcceptanceEventDto.driverFirstname(),
                        emailDeliveryAcceptanceEventDto.driverLastname(),
                        emailDeliveryAcceptanceEventDto.driverPhoneNumber(),
                        emailDeliveryAcceptanceEventDto.vehicleLicenseNumber(),
                        emailDeliveryAcceptanceEventDto.pickupDeliveryCode(),
                        emailDeliveryAcceptanceEventDto.dropoffDeliveryCode(),
                        emailDeliveryAcceptanceEventDto.orderReferenceNumber(),
                        true
                );

                EmailRequestDto buyerEmailRequestDto = EmailRequestDto
                        .builder()
                        .recipient(emailDeliveryAcceptanceEventDto.buyerEmail())
                        .subject("Your delivery has been accepted by a driver")
                        .messageBody(mailBody)
                        .build();

                log.info("Sending delivery acceptance email to buyer: {} for order reference number: {}",
                        emailDeliveryAcceptanceEventDto.buyerEmail(), emailDeliveryAcceptanceEventDto.orderReferenceNumber());

                emailService.sendEmail(buyerEmailRequestDto);

                log.info("Delivery acceptance email sent to buyer: {} for order reference number: {}",
                        emailDeliveryAcceptanceEventDto.buyerEmail(), emailDeliveryAcceptanceEventDto.orderReferenceNumber());

            } catch (Exception ex) {

                log.error("Failed to process delivery acceptance email for buyer with email: {} and driver with phone number: {} — {}",
                        emailDeliveryAcceptanceEventDto.buyerEmail(), emailDeliveryAcceptanceEventDto.driverPhoneNumber(), ex.getMessage(), ex);

                throw ex;
            }
        };
    }

    @Bean
    public Consumer<EmailDeliveryAcceptanceEventDto> sendMailToSellerAfterDriverAcceptsDelivery() {

        return emailDeliveryAcceptanceEventDto -> {

            try {

                String mailBody = AppExtensions.getEmailDeliveryAcceptanceMailBody(
                        emailDeliveryAcceptanceEventDto.driverFirstname(),
                        emailDeliveryAcceptanceEventDto.driverLastname(),
                        emailDeliveryAcceptanceEventDto.driverPhoneNumber(),
                        emailDeliveryAcceptanceEventDto.vehicleLicenseNumber(),
                        emailDeliveryAcceptanceEventDto.pickupDeliveryCode(),
                        emailDeliveryAcceptanceEventDto.dropoffDeliveryCode(),
                        emailDeliveryAcceptanceEventDto.orderReferenceNumber(),
                        false
                );

                EmailRequestDto sellerEmailRequestDto = EmailRequestDto
                        .builder()
                        .recipient(emailDeliveryAcceptanceEventDto.sellerEmail())
                        .subject("Your delivery has been accepted by a driver")
                        .messageBody(mailBody)
                        .build();

                log.info("Sending delivery acceptance email to seller: {} for order reference number: {}",
                        emailDeliveryAcceptanceEventDto.sellerEmail(), emailDeliveryAcceptanceEventDto.orderReferenceNumber());

                emailService.sendEmail(sellerEmailRequestDto);

                log.info("Delivery acceptance email sent to seller: {} for order reference number: {}",
                        emailDeliveryAcceptanceEventDto.sellerEmail(), emailDeliveryAcceptanceEventDto.orderReferenceNumber());

            } catch (Exception ex) {

                log.error("Failed to process delivery acceptance email for seller with email: {} and driver with phone number: {} — {}",
                        emailDeliveryAcceptanceEventDto.sellerEmail(), emailDeliveryAcceptanceEventDto.driverPhoneNumber(), ex.getMessage(), ex);

                throw ex;
            }
        };
    }

    @Bean
    public Consumer<EscrowReleaseCompletedEventDto> sendEscrowReleaseMailToSeller() {

        return escrowReleaseCompletedEventDto -> {

            try {

                String mailBody = AppExtensions.getEscrowReleaseMailBody(
                        escrowReleaseCompletedEventDto.orderReferenceNumber(),
                        escrowReleaseCompletedEventDto.amount().toPlainString(),
                        escrowReleaseCompletedEventDto.buyerAvailableBalance().toPlainString(),
                        escrowReleaseCompletedEventDto.sellerAvailableBalance().toPlainString(),
                        false
                );

                EmailRequestDto emailRequestDto = EmailRequestDto
                        .builder()
                        .recipient(escrowReleaseCompletedEventDto.sellerEmail())
                        .subject("Escrow Release Completed. You have been credited")
                        .messageBody(mailBody)
                        .build();

                log.info("Sending escrow release confirmation email to: {} for order reference number: {}",
                        escrowReleaseCompletedEventDto.sellerEmail(),
                        escrowReleaseCompletedEventDto.orderReferenceNumber()
                );

                emailService.sendEmail(emailRequestDto);

                log.info("Escrow release confirmation email sent to: {} for order reference number: {}",
                        escrowReleaseCompletedEventDto.sellerEmail(),
                        escrowReleaseCompletedEventDto.orderReferenceNumber()
                );

            } catch (Exception e) {

                log.error("Failed to process escrow release confirmation email for: {} — {}", escrowReleaseCompletedEventDto.sellerEmail(), e.getMessage(), e);

                throw e;
            }
        };
    }

    @Bean
    public Consumer<DriverWalletCreditNotification> sendEmailToDriverAfterWalletCredit() {

        return driverWalletCreditNotification -> {

            try {

                String mailBody = AppExtensions.getDriverWalletCreditMailBody(
                        driverWalletCreditNotification.amount().toPlainString(),
                        driverWalletCreditNotification.availableBalance().toPlainString(),
                        driverWalletCreditNotification.orderReferenceNumber()
                );

                EmailRequestDto emailRequestDto = EmailRequestDto
                        .builder()
                        .recipient(driverWalletCreditNotification.driverEmail())
                        .subject("Your wallet has been credited")
                        .messageBody(mailBody)
                        .build();

                log.info("Sending wallet credit notification email to driver: {} for order reference number: {}",
                        driverWalletCreditNotification.driverEmail(),
                        driverWalletCreditNotification.orderReferenceNumber()
                );

                emailService.sendEmail(emailRequestDto);

                log.info("Wallet credit notification email sent to driver: {} for order reference number: {}",
                        driverWalletCreditNotification.driverEmail(),
                        driverWalletCreditNotification.orderReferenceNumber()
                );

            } catch (Exception e) {

                log.error("Failed to process wallet credit notification email for driver: {} — {}",
                        driverWalletCreditNotification.driverEmail(), e.getMessage(), e);

                throw e;
            }
        };
    }
}
