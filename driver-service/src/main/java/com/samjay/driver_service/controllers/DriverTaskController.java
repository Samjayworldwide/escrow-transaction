package com.samjay.driver_service.controllers;

import com.samjay.driver_service.dtos.requests.DeliveryCodeVerificationRequest;
import com.samjay.driver_service.dtos.requests.FetchDriverTaskRequest;
import com.samjay.driver_service.dtos.responses.ApiResponse;
import com.samjay.driver_service.dtos.responses.CursorPaginatedResponse;
import com.samjay.driver_service.dtos.responses.DriverTaskResponse;
import com.samjay.driver_service.services.interfaces.DriverTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.samjay.driver_service.utility.AppExtensions.CLIENT_REQUEST_KEY_HEADER;

@SuppressWarnings("NullableProblems")
@RestController
@RequestMapping("/api/driver-tasks")
@RequiredArgsConstructor
public class DriverTaskController {

    private final DriverTaskService driverTaskService;

    @PostMapping("/verify-delivery-code")
    public ResponseEntity<ApiResponse<String>> verifyDeliveryCode(@RequestHeader(CLIENT_REQUEST_KEY_HEADER) String clientRequestKey,
                                                                  @Valid @RequestBody DeliveryCodeVerificationRequest deliveryCodeVerificationRequest) {

        ApiResponse<String> apiResponse = driverTaskService.verifyDeliveryCode(clientRequestKey, deliveryCodeVerificationRequest);

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);

    }


    @GetMapping("/get-tasks")
    public ResponseEntity<ApiResponse<CursorPaginatedResponse<DriverTaskResponse>>> fetchDriverTasks(@RequestParam(value = "cursor", required = false) String cursor,
                                                                                                     @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {

        FetchDriverTaskRequest fetchDriverTaskRequest = new FetchDriverTaskRequest(cursor, pageSize);

        ApiResponse<CursorPaginatedResponse<DriverTaskResponse>> apiResponse = driverTaskService.fetchDriverTasks(fetchDriverTaskRequest);

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);
    }
}
