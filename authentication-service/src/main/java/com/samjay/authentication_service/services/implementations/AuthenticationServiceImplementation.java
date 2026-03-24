package com.samjay.authentication_service.services.implementations;

import com.samjay.authentication_service.dtos.events.UserRegisteredEventDto;
import com.samjay.authentication_service.dtos.requests.UserLoginRequest;
import com.samjay.authentication_service.dtos.requests.UserRegistrationRequest;
import com.samjay.authentication_service.dtos.responses.ApiResponse;
import com.samjay.authentication_service.dtos.responses.LoginResponse;
import com.samjay.authentication_service.entities.User;
import com.samjay.authentication_service.enumerations.Roles;
import com.samjay.authentication_service.repositories.UserRepository;
import com.samjay.authentication_service.security.interfaces.JwtService;
import com.samjay.authentication_service.services.interfaces.AuthenticationService;
import com.samjay.authentication_service.services.interfaces.EmailVerificationService;
import com.samjay.authentication_service.services.interfaces.IdempotencyService;
import com.samjay.authentication_service.services.interfaces.OutboxEventService;
import com.samjay.authentication_service.utils.AppExtensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.samjay.authentication_service.utils.AppExtensions.generateHash;
import static com.samjay.authentication_service.utils.AppExtensions.serialize;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImplementation implements AuthenticationService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailVerificationService emailVerificationService;

    private final OutboxEventService outboxEventService;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final IdempotencyService idempotencyService;

    @Transactional
    @Override
    public ApiResponse<String> registerUser(UserRegistrationRequest userRegistrationRequest, String clientRequestKey) {

        String requestFingerprint = serialize(userRegistrationRequest);

        String hashedFingerprint = generateHash(Objects.requireNonNull(requestFingerprint));

        Optional<ApiResponse<String>> existingResponse = idempotencyService.checkKey(
                clientRequestKey,
                AppExtensions.USER_REGISTERED_EVENT_TYPE,
                hashedFingerprint,
                String.class
        );

        if (existingResponse.isPresent())
            return existingResponse.get();

        boolean isEmailVerified = emailVerificationService.isEmailVerified(userRegistrationRequest.getEmail());

        if (!isEmailVerified)
            return ApiResponse.error("Email address is not verified. Please verify your email address before registering.");

        boolean isPasswordMatching = userRegistrationRequest.getPassword().trim().equals(userRegistrationRequest.getConfirmPassword().trim());

        if (!isPasswordMatching)
            return ApiResponse.error("Passwords do not match");

        boolean existsByEmail = userRepository.existsByEmail(userRegistrationRequest.getEmail());

        if (existsByEmail)
            return ApiResponse.error("Email address already exists");

        if (userRegistrationRequest.getRole() == Roles.CUSTOMER) {

            boolean existsByUsername = userRepository.existsByUsername(userRegistrationRequest.getUsername());

            if (existsByUsername)
                return ApiResponse.error("Username already exists");
        }

        int insertedIdempotentRecord = idempotencyService.saveKey(
                clientRequestKey,
                userRegistrationRequest.getEmail(),
                AppExtensions.USER_REGISTERED_EVENT_TYPE,
                hashedFingerprint
        );

        if (insertedIdempotentRecord == 0) {

            log.warn("Idempotency key {} is already being processed for email {}. Registration skipped.", clientRequestKey, userRegistrationRequest.getEmail());

            return ApiResponse.error("Request is already being processed. Please wait.");
        }

        UUID id = UUID.randomUUID();

        String encodedPassword = passwordEncoder.encode(userRegistrationRequest.getPassword());

        int rowsInserted = userRepository.insertIgnoreConflict(
                id,
                userRegistrationRequest.getEmail(),
                userRegistrationRequest.getUsername(),
                encodedPassword,
                userRegistrationRequest.getRole().name(),
                LocalDateTime.now()
        );

        if (rowsInserted == 0) {

            log.warn("Conflict on insert for email {} — concurrent duplicate request", userRegistrationRequest.getEmail());

            idempotencyService.markKeyAsFailed(clientRequestKey, AppExtensions.USER_REGISTERED_EVENT_TYPE);

            return ApiResponse.error("Email or username already exists");
        }

        UserRegisteredEventDto userRegisteredEventDto = new UserRegisteredEventDto(
                id,
                userRegistrationRequest.getFirstname(),
                userRegistrationRequest.getLastname(),
                userRegistrationRequest.getEmail(),
                userRegistrationRequest.getUsername(),
                userRegistrationRequest.getRole()
        );

        outboxEventService.saveEvent(
                userRegistrationRequest.getEmail(),
                AppExtensions.USER_REGISTERED_EVENT_TYPE,
                AppExtensions.USER_REGISTERED_KAFKA_BINDING,
                userRegisteredEventDto,
                clientRequestKey
        );

        idempotencyService.markKeyAsSuccess(
                clientRequestKey,
                AppExtensions.USER_REGISTERED_EVENT_TYPE,
                "Registration successful",
                null
        );

        return ApiResponse.success("Registration successful");

    }

    @Transactional
    @Override
    public ApiResponse<LoginResponse> loginUser(UserLoginRequest userLoginRequest) {

        Optional<User> userOptional = userRepository.findByEmail(userLoginRequest.getEmail());

        if (userOptional.isEmpty())
            return ApiResponse.error("Invalid email or password");

        User user = userOptional.get();

        if (user.isAccountLocked())
            return ApiResponse.error("Your account is locked. Please try resetting your password or contact support.");

        boolean isPasswordMatching = passwordEncoder.matches(userLoginRequest.getPassword(), user.getPassword());

        if (!isPasswordMatching) {

            int currentAttempts = user.getFailedLoginAttempts();

            int updatedAttempts = currentAttempts + 1;

            user.setFailedLoginAttempts(updatedAttempts);

            int remainingAttempts = AppExtensions.MAX_LOGIN_ATTEMPTS - updatedAttempts;

            if (remainingAttempts <= 0) {

                user.setAccountLocked(true);

                userRepository.save(user);

                return ApiResponse.error("Your account has been locked due to too many failed login attempts. Please reset your password or contact support.");

            }

            userRepository.save(user);

            return ApiResponse.error("Invalid email or password. You have " + remainingAttempts + " more attempt(s) before your account gets locked.");
        }

        user.setFailedLoginAttempts(0);

        user.setAccountLocked(false);

        var usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userLoginRequest.getEmail(), userLoginRequest.getPassword());

        Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);

        String jwtToken = jwtService.generateToken(authentication);

        LoginResponse loginResponse = new LoginResponse(user.getUsername(), jwtToken, LocalDateTime.now());

        userRepository.save(user);

        return ApiResponse.success("Login successful", loginResponse);

    }
}
