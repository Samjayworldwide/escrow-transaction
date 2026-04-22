package com.samjay.driver_service.controllers;

import com.samjay.driver_service.dtos.requests.CompleteProfileRequest;
import com.samjay.driver_service.dtos.responses.ApiResponse;
import com.samjay.driver_service.models.DriverLocation;
import com.samjay.driver_service.services.interfaces.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.samjay.driver_service.utility.AppExtensions.CLIENT_REQUEST_KEY_HEADER;

@SuppressWarnings("ALL")
@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping(value = "/complete-profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> completeProfile(@Valid @ModelAttribute CompleteProfileRequest completeProfileRequest) {

        ApiResponse<String> apiResponse = driverService.completeProfile(completeProfileRequest);

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);

    }

    @PostMapping("/accept-delivery-request")
    public ResponseEntity<ApiResponse<String>> acceptDeliveryRequest(@RequestHeader(CLIENT_REQUEST_KEY_HEADER) String clientRequestKey,
                                                                     @RequestParam("orderId") UUID orderId) {

        ApiResponse<String> apiResponse = driverService.acceptDeliveryRequest(clientRequestKey, orderId);

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);

    }

    @GetMapping("/nearby-drivers")
    public ResponseEntity<ApiResponse<List<DriverLocation>>> findNearbyDrivers(@RequestParam("orderId") UUID orderId) {

        ApiResponse<List<DriverLocation>> apiResponse = driverService.searchForNearbyDrivers(orderId);

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);

    }
}
