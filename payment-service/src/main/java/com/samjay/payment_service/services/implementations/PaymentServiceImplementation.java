package com.samjay.payment_service.services.implementations;

import com.samjay.FetchOrderDetailsResponse;
import com.samjay.payment_service.configuration.AuthenticatedUserProvider;
import com.samjay.payment_service.dtos.events.PaymentVerificationEventDto;
import com.samjay.payment_service.dtos.events.PaymentInitializationEventDto;
import com.samjay.payment_service.dtos.requests.PaymentRequest;
import com.samjay.payment_service.dtos.responses.ApiResponse;
import com.samjay.payment_service.dtos.responses.PaymentInitializationResponse;
import com.samjay.payment_service.dtos.responses.PaystackInitializationResponse;
import com.samjay.payment_service.dtos.responses.UserIdentifier;
import com.samjay.payment_service.entities.Payment;
import com.samjay.payment_service.enumerations.TransactionStatus;
import com.samjay.payment_service.repositories.PaymentRepository;
import com.samjay.payment_service.services.grpcservice.OrderService;
import com.samjay.payment_service.services.interfaces.IdempotencyService;
import com.samjay.payment_service.services.interfaces.OutboxEventService;
import com.samjay.payment_service.services.interfaces.PayStackService;
import com.samjay.payment_service.services.interfaces.PaymentService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.samjay.payment_service.utility.AppExtensions.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImplementation implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final IdempotencyService idempotencyService;

    private final OrderService orderService;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    private final PayStackService payStackService;

    private final OutboxEventService outboxEventService;

    @Value("${app.callback-url}")
    private String callbackUrl;

    @Transactional
    @Override
    public ApiResponse<PaymentInitializationResponse> initializePayment(String clientRequestKey, PaymentRequest paymentRequest) {

        UserIdentifier userIdentifier = authenticatedUserProvider.getCurrentLoggedInUser();

        String fingerPrint = serialize(paymentRequest);

        String requestFingerPrint = generateHash(Objects.requireNonNull(fingerPrint));

        Optional<ApiResponse<PaymentInitializationResponse>> idempotencyCheck = idempotencyService.checkKey(
                clientRequestKey,
                PAYMENT_INITIALIZATION_EVENT_TYPE,
                requestFingerPrint,
                PaymentInitializationResponse.class
        );

        if (idempotencyCheck.isPresent())
            return idempotencyCheck.get();

        ApiResponse<FetchOrderDetailsResponse> fetchOrderDetailsResponse = orderService.fetchOrderDetails(
                paymentRequest.getOrderId().toString(),
                userIdentifier.userId()
        );

        if (!fetchOrderDetailsResponse.isSuccessful())
            return ApiResponse.error("Unable to fetch the total price for all items in order. Please try again later.");

        boolean isOrderFound = fetchOrderDetailsResponse.getResponseBody().getIsFound();

        if (!isOrderFound)
            return ApiResponse.error("Order not found for the provided order ID. Please check your order and try again.");

        boolean isOrderApproved = fetchOrderDetailsResponse.getResponseBody().getIsApproved();

        if (!isOrderApproved)
            return ApiResponse.error("This order is not approved yet. You can only make payment for approved orders. " +
                    "Please wait for your order to be approved before making payment.");

        boolean isPaidByBuyer = fetchOrderDetailsResponse.getResponseBody().getIsPaidByBuyer();

        if (!isPaidByBuyer)
            return ApiResponse.error("This order can only be paid by the buyer");

        double totalPriceOfItems = fetchOrderDetailsResponse.getResponseBody().getTotalPriceOfItems();

        BigDecimal itemsAmountBigDecimal = BigDecimal.valueOf(totalPriceOfItems);

        if (paymentRequest.getAmount().compareTo(itemsAmountBigDecimal) != 0)
            return ApiResponse.error("Payment amount is invalid. Please pay the right amount for this order");

        int idempotencyInsertedRow = idempotencyService.saveKey(
                clientRequestKey,
                PAYMENT_INITIALIZATION_EVENT_TYPE,
                requestFingerPrint
        );

        if (idempotencyInsertedRow == 0) {

            log.info("Duplicate request detected for client request key: {}. Another request with the same key is currently being processed.", clientRequestKey);

            return ApiResponse.success("Another request with the same client request key is currently being processed. Please wait for it to complete.");
        }

        UUID paymentId = UUID.randomUUID();

        callbackUrl = callbackUrl + "?clientRequestKey=" + clientRequestKey + "&paymentId=" + paymentId;

        ApiResponse<PaystackInitializationResponse> response = payStackService.paystackInitialization(
                userIdentifier.email(),
                paymentRequest.getAmount(),
                callbackUrl
        );

        if (!response.isSuccessful())
            return ApiResponse.error("Failed to initialize payment transaction. Please try again later.");

        PaystackInitializationResponse paystackInitializationResponse = response.getResponseBody();

        int paymentRecordInserted = paymentRepository.insertPaymentRecord(
                paymentId,
                paymentRequest.getAmount(),
                paymentRequest.getOrderId(),
                UUID.fromString(userIdentifier.userId()),
                paystackInitializationResponse.getReference(),
                paymentRequest.getDescription(),
                TransactionStatus.PENDING.name(),
                0L
        );

        if (paymentRecordInserted == 0) {

            log.info(
                    "Payment record with reference {} already exists. This is likely a duplicate request.",
                    paystackInitializationResponse.getReference()
            );

            idempotencyService.markKeyAsFailed(clientRequestKey, PAYMENT_INITIALIZATION_EVENT_TYPE);

            return ApiResponse.error(
                    "Payment with the same reference already exists. This is likely a duplicate request." +
                            " Please check your payment history or try again later."
            );
        }

        PaymentInitializationResponse paymentInitializationResponse = new PaymentInitializationResponse(
                paymentId,
                paystackInitializationResponse.getAccessCode(),
                paystackInitializationResponse.getAuthorizationUrl()
        );

        PaymentInitializationEventDto paymentInitializationEventDto = new PaymentInitializationEventDto(
                userIdentifier.email(),
                paystackInitializationResponse.getAuthorizationUrl(),
                fetchOrderDetailsResponse.getResponseBody().getOrderRefrenceNumber()
        );

        outboxEventService.saveEvent(
                userIdentifier.userId(),
                PAYMENT_INITIALIZATION_EVENT_TYPE,
                PAYMENT_INITIALIZATION_KAFKA_BINDING,
                paymentInitializationEventDto,
                clientRequestKey
        );

        idempotencyService.markKeyAsSuccess(
                clientRequestKey,
                PAYMENT_INITIALIZATION_EVENT_TYPE,
                "Payment transaction initialized successfully.",
                paymentInitializationResponse
        );

        return ApiResponse.success("Please check your email address to complete payment", paymentInitializationResponse);
    }

    @Transactional
    @Override
    public ApiResponse<String> verifyPayment(String clientRequestKey, UUID paymentId) {

        try {

            String requestFingerPrint = generateHash(Objects.requireNonNull(paymentId.toString()));

            Optional<ApiResponse<String>> idempotencyCheck = idempotencyService.checkKey(
                    clientRequestKey,
                    PAYMENT_VERIFICATION_EVENT_TYPE,
                    requestFingerPrint,
                    String.class
            );

            if (idempotencyCheck.isPresent())
                return idempotencyCheck.get();

            Optional<Payment> optionalPayment = paymentRepository.findById(paymentId);

            if (optionalPayment.isEmpty())
                return ApiResponse.error("Payment record not found for the provided payment ID and reference.");

            Payment payment = optionalPayment.get();

            if (payment.getTransactionStatus() == TransactionStatus.SUCCESS)
                return ApiResponse.success("Payment has already been verified successfully.");

            int idempotencyInsertedRow = idempotencyService.saveKey(
                    clientRequestKey,
                    PAYMENT_VERIFICATION_EVENT_TYPE,
                    requestFingerPrint
            );

            if (idempotencyInsertedRow == 0) {

                log.info("Duplicate request detected for client request key: {}. Another request with the same key is currently being processed.", clientRequestKey);

                return ApiResponse.success("Another request with the same client request key is currently being processed. Please wait for it to complete.");
            }

            ApiResponse<String> verificationResponse = payStackService.paystackVerifyPayment(payment.getPaymentReference());

            if (!verificationResponse.isSuccessful())
                return ApiResponse.error("Failed to verify payment transaction. Please try again later.");

            String verificationMessage = verificationResponse.getResponseBody();

            if (!verificationMessage.equalsIgnoreCase("success")) {

                payment.setTransactionStatus(TransactionStatus.FAILED);

                paymentRepository.save(payment);

                idempotencyService.markKeyAsFailed(clientRequestKey, PAYMENT_VERIFICATION_EVENT_TYPE);

                return ApiResponse.error("Payment verification failed. The transaction was not successful.");
            }

            payment.setTransactionStatus(TransactionStatus.SUCCESS);

            paymentRepository.save(payment);

            PaymentVerificationEventDto paymentVerificationEventDto = new PaymentVerificationEventDto(
                    payment.getOrderId(),
                    payment.getId(),
                    payment.getAmount(),
                    clientRequestKey
            );

            outboxEventService.saveEvent(payment.getUserId().toString(),
                    PAYMENT_VERIFICATION_EVENT_TYPE,
                    PAYMENT_VERIFICATION_KAFKA_BINDING,
                    paymentVerificationEventDto,
                    clientRequestKey
            );

            idempotencyService.markKeyAsSuccess(clientRequestKey, PAYMENT_VERIFICATION_EVENT_TYPE, "Payment verified successfully.", null);

            return ApiResponse.success("Payment verified successfully.");

        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {

            log.error(
                    "Optimistic lock exception occurred while verifying payment ID   {}: {}",
                    paymentId,
                    ex.getMessage(),
                    ex
            );

            Optional<Payment> optionalPayment = paymentRepository.findById(paymentId);

            if (optionalPayment.isEmpty())
                return ApiResponse.error("Payment record not found for the provided payment ID and reference.");

            Payment latestPayment = optionalPayment.get();

            if (latestPayment.getTransactionStatus() == TransactionStatus.SUCCESS)
                return ApiResponse.success("Payment already verified successfully.");

            return ApiResponse.error("Payment verification failed. Please try again later.");
        }
    }
}
