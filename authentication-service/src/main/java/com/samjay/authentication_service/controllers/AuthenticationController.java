package com.samjay.authentication_service.controllers;

import com.samjay.authentication_service.dtos.requests.UserLoginRequest;
import com.samjay.authentication_service.dtos.requests.UserRegistrationRequest;
import com.samjay.authentication_service.dtos.responses.ApiResponse;
import com.samjay.authentication_service.dtos.responses.LoginResponse;
import com.samjay.authentication_service.services.interfaces.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.samjay.authentication_service.utils.AppExtensions.CLIENT_REQUEST_KEY_HEADER;

@SuppressWarnings("NullableProblems")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> registerUser(@Valid @RequestBody UserRegistrationRequest userRegistrationRequest,
                                                            @RequestHeader(value = CLIENT_REQUEST_KEY_HEADER) String clientRequestKey) {

        ApiResponse<String> response = authenticationService.registerUser(userRegistrationRequest, clientRequestKey);

        if (!response.isSuccessful())
            return ResponseEntity.badRequest().body(response);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginUser(@Valid @RequestBody UserLoginRequest userLoginRequest) {

        ApiResponse<LoginResponse> response = authenticationService.loginUser(userLoginRequest);

        if (!response.isSuccessful())
            return ResponseEntity.badRequest().body(response);

        return ResponseEntity.ok(response);
    }
}
