package com.samjay.authentication_service.services.interfaces;

import com.samjay.authentication_service.dtos.requests.UserLoginRequest;
import com.samjay.authentication_service.dtos.requests.UserRegistrationRequest;
import com.samjay.authentication_service.dtos.responses.ApiResponse;
import com.samjay.authentication_service.dtos.responses.LoginResponse;

public interface AuthenticationService {

    ApiResponse<String> registerUser(UserRegistrationRequest userRegistrationRequest, String clientRequestKey);

    ApiResponse<LoginResponse> loginUser(UserLoginRequest userLoginRequest);
}
