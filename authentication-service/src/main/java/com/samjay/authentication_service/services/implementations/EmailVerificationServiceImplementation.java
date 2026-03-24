package com.samjay.authentication_service.services.implementations;

import com.samjay.authentication_service.dtos.events.EmailVerificationEventDto;
import com.samjay.authentication_service.dtos.requests.EmailVerificationRequest;
import com.samjay.authentication_service.dtos.responses.ApiResponse;
import com.samjay.authentication_service.entities.EmailVerification;
import com.samjay.authentication_service.repositories.EmailVerificationRepository;
import com.samjay.authentication_service.services.interfaces.EmailVerificationService;
import com.samjay.authentication_service.services.interfaces.EmailVerificationUpsertService;
import com.samjay.authentication_service.services.interfaces.IdempotencyService;
import com.samjay.authentication_service.services.interfaces.OutboxEventService;
import com.samjay.authentication_service.utils.AppExtensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImplementation implements EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;

    private final PasswordEncoder passwordEncoder;

    private final OutboxEventService outboxEventService;

    private final EmailVerificationUpsertService emailVerificationUpsertService;

    private final IdempotencyService idempotencyService;

    @Transactional
    @Override
    public ApiResponse<String> sendVerificationEmail(String email, String clientRequestKey) {

        String hashedFingerprint = AppExtensions.generateHash(email);

        Optional<ApiResponse<String>> existingResponse = idempotencyService.checkKey(
                clientRequestKey,
                AppExtensions.EMAIL_VERIFICATION_EVENT_TYPE,
                hashedFingerprint,
                String.class
        );

        if (existingResponse.isPresent())
            return existingResponse.get();

        String verificationCode = AppExtensions.generateVerificationCode();

        String token = passwordEncoder.encode(verificationCode);

        int insertedIdempotentRecord = idempotencyService.saveKey(
                clientRequestKey,
                email,
                AppExtensions.EMAIL_VERIFICATION_EVENT_TYPE,
                hashedFingerprint
        );

        if (insertedIdempotentRecord == 0) {

            log.info("Another request with the same idempotency key is already being processed. ClientRequestKey: {}", clientRequestKey);

            return ApiResponse.success("A verification email is already being sent to this email address. Please wait a moment and try again.");
        }

        boolean isUpserted = emailVerificationUpsertService.upsertEmailVerification(email, token);

        if (!isUpserted) {

            log.info("Concurrent request detected for email {}. ClientRequestKey: {}. Another request has already created a verification record for this email.", email, clientRequestKey);

            return ApiResponse.success("A verification email is already being sent to this email address. Please wait a moment and try again.");
        }

        EmailVerificationEventDto emailVerificationEventDto = new EmailVerificationEventDto(email, verificationCode);

        outboxEventService.saveEvent(
                email,
                AppExtensions.EMAIL_VERIFICATION_EVENT_TYPE,
                AppExtensions.EMAIL_VERIFICATION_KAFKA_BINDING,
                emailVerificationEventDto,
                clientRequestKey
        );

        idempotencyService.markKeyAsSuccess(
                clientRequestKey,
                AppExtensions.EMAIL_VERIFICATION_EVENT_TYPE,
                "A verification email has been sent to your email address",
                null
        );

        return ApiResponse.success("A verification email has been sent to your email address");
    }

    @Override
    @Transactional
    public ApiResponse<String> verifyEmail(EmailVerificationRequest emailVerificationRequest) {

        try {

            Optional<EmailVerification> optional = emailVerificationRepository.findByEmail(emailVerificationRequest.getEmail());

            if (optional.isEmpty())
                return ApiResponse.error("No verification record found for the provided email");

            EmailVerification emailVerification = optional.get();

            if (emailVerification.getExpiresAt().isBefore(AppExtensions.getCurrentDateTime()))
                return ApiResponse.error("Verification code has expired. Please request a new one.");

            if (!passwordEncoder.matches(emailVerificationRequest.getVerificationCode(), emailVerification.getToken()))
                return ApiResponse.error("Invalid verification code. Please check the code and try again.");

            emailVerification.setVerified(true);

            emailVerificationRepository.save(emailVerification);

            return ApiResponse.success("Email verified successfully");

        } catch (Exception ex) {

            log.error("An unexpected error occurred verifying token {}", ex.getMessage(), ex);

            throw ex;

        }
    }

    @Override
    public boolean isEmailVerified(String email) {

        Optional<EmailVerification> optional = emailVerificationRepository.findByEmail(email);

        return optional.map(EmailVerification::isVerified).orElse(false);
    }
}
