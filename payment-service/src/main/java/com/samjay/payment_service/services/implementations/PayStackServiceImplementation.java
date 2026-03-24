package com.samjay.payment_service.services.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samjay.payment_service.dtos.requests.PaystackInitializationRequest;
import com.samjay.payment_service.dtos.responses.ApiResponse;
import com.samjay.payment_service.dtos.responses.PaystackInitializationResponse;
import com.samjay.payment_service.services.interfaces.PayStackService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Service
@RequiredArgsConstructor
@Slf4j
public class PayStackServiceImplementation implements PayStackService {

    @Value("${paystack.base-url}")
    private String paystackBaseUrl;

    @Value("${paystack.secret-key}")
    private String paystackSecretKey;

    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate;


    @CircuitBreaker(name = "interServiceCircuit", fallbackMethod = "paystackInitializationFallback")
    @Retry(name = "interServiceRetry", fallbackMethod = "paystackInitializationFallback")
    @Bulkhead(name = "interServiceBulkhead", fallbackMethod = "paystackInitializationFallback", type = Bulkhead.Type.SEMAPHORE)
    @Override
    public ApiResponse<PaystackInitializationResponse> paystackInitialization(String email, BigDecimal amount, String callBackUrl) {

        try {

            long formattedAmount = amount
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();

            String url = paystackBaseUrl + "/transaction/initialize";

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.setBearerAuth(paystackSecretKey);

            String reference = generateUniqueReference();

            log.info("Paystack initialization request reference generated: {}", reference);

            PaystackInitializationRequest paystackInitializationRequest = new PaystackInitializationRequest(email, formattedAmount, callBackUrl, reference);

            HttpEntity<PaystackInitializationRequest> request = new HttpEntity<>(paystackInitializationRequest, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {

                log.error("Failed to verify Paystack transaction. HTTP Status: {}", response.getStatusCode());

                return ApiResponse.error("Failed to initialize payment transaction. Please try again later.");
            }

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            if (!jsonNode.path("status").asBoolean()) {

                log.error("Failed to verify Paystack transaction. Response message: {}", jsonNode.path("message").asText());

                return ApiResponse.error(jsonNode.path("message").asText());
            }

            String authorizationUrl = jsonNode.path("data").path("authorization_url").asText();

            String accessCode = jsonNode.path("data").path("access_code").asText();

            String paystackReference = jsonNode.path("data").path("reference").asText();

            log.info("Paystack initialization response reference received: {}", paystackReference);

            PaystackInitializationResponse paystackInitializationResponse = new PaystackInitializationResponse(
                    accessCode,
                    paystackReference,
                    authorizationUrl
            );

            return ApiResponse.success("Payment transaction initialized successfully.", paystackInitializationResponse);

        } catch (Exception e) {

            log.error("Error initializing Paystack transaction: {}", e.getMessage(), e);

            throw new RuntimeException(e);
        }
    }

    @CircuitBreaker(name = "interServiceCircuit", fallbackMethod = "paystackVerifyPaymentFallback")
    @Retry(name = "interServiceRetry", fallbackMethod = "paystackVerifyPaymentFallback")
    @Bulkhead(name = "interServiceBulkhead", fallbackMethod = "paystackVerifyPaymentFallback", type = Bulkhead.Type.SEMAPHORE)
    @Override
    public ApiResponse<String> paystackVerifyPayment(String reference) {

        try {

            String url = paystackBaseUrl + "/transaction/verify/" + reference;

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.setBearerAuth(paystackSecretKey);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {

                log.error("Failed to verify Paystack transaction. HTTP Status: {}", response.getStatusCode());

                return ApiResponse.error("Failed to initialize payment transaction. Please try again later.");
            }

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            if (!jsonNode.path("status").asBoolean()) {

                log.error("Failed to verify Paystack transaction. Response message: {}", jsonNode.path("message").asText());

                return ApiResponse.error(jsonNode.path("message").asText());
            }

            String transactionStatus = jsonNode.path("data").path("status").asText();

            log.info("Paystack transaction status received: {}", transactionStatus);

            return ApiResponse.success("Payment transaction verified successfully.", transactionStatus);

        } catch (Exception e) {

            log.error("Error verifying Paystack transaction: {}", e.getMessage(), e);

            throw new RuntimeException(e);
        }
    }

    public ApiResponse<PaystackInitializationResponse> paystackInitializationFallback(String email, BigDecimal amount, String callBackUrl, Throwable throwable) {

        log.error("Fallback executed for Paystack initialization with email: {}, amount: {}, callBackUrl: {}. Reason: {}", email, amount, callBackUrl, throwable.getMessage(), throwable);

        return ApiResponse.error("Failed to initialize payment transaction due to a service issue. Please try again later.");
    }

    public ApiResponse<String> paystackVerifyPaymentFallback(String reference, Throwable throwable) {

        log.error("Fallback executed for Paystack verification with reference: {}. Reason: {}", reference, throwable.getMessage(), throwable);

        return ApiResponse.error("Failed to verify payment transaction due to a service issue. Please try again later.");
    }

    private String generateUniqueReference() {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String guid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        return "REF-" + timestamp + "-" + guid;
    }
}
