package com.samjay.driver_service.controllers;

import com.samjay.driver_service.dtos.requests.CompleteProfileRequest;
import com.samjay.driver_service.dtos.responses.ApiResponse;
import com.samjay.driver_service.services.interfaces.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("ALL")
@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping(value = "/complete-profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> completeProfile(@Valid @ModelAttribute CompleteProfileRequest completeProfileRequest) {

        ApiResponse<String> response = driverService.completeProfile(completeProfileRequest);

        if (!response.isSuccessful())
            return ResponseEntity.badRequest().body(response);

        return ResponseEntity.ok(response);
    }

}
