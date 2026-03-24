package com.samjay.authentication_service.services.interfaces;

import com.samjay.authentication_service.dtos.requests.EmailVerificationRequest;
import com.samjay.authentication_service.dtos.responses.ApiResponse;

public interface EmailVerificationService {

    ApiResponse<String> sendVerificationEmail(String email, String clientRequestKey);

    ApiResponse<String> verifyEmail(EmailVerificationRequest emailVerificationRequest);

    boolean isEmailVerified(String email);
}
