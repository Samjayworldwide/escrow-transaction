package com.samjay.authentication_service.controllers;

import com.samjay.authentication_service.dtos.requests.EmailVerificationRequest;
import com.samjay.authentication_service.dtos.responses.ApiResponse;
import com.samjay.authentication_service.services.interfaces.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.samjay.authentication_service.utils.AppExtensions.CLIENT_REQUEST_KEY_HEADER;

@SuppressWarnings("NullableProblems")
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/send-verification")
    public ResponseEntity<ApiResponse<String>> sendVerificationEmail(@RequestParam String email,
                                                                     @RequestHeader(value = CLIENT_REQUEST_KEY_HEADER) String clientRequestKey) {

        if (email == null || email.isEmpty())
            return ResponseEntity.badRequest().body(ApiResponse.error("Email address is required"));

        if (clientRequestKey == null || clientRequestKey.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("X-Client-Request-Key header is required"));

        ApiResponse<String> response = emailVerificationService.sendVerificationEmail(email, clientRequestKey);

        if (!response.isSuccessful())
            return ResponseEntity.badRequest().body(response);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@Valid @RequestBody EmailVerificationRequest emailVerificationRequest) {

        ApiResponse<String> response = emailVerificationService.verifyEmail(emailVerificationRequest);

        if (!response.isSuccessful())
            return ResponseEntity.badRequest().body(response);

        return ResponseEntity.ok(response);
    }
}
