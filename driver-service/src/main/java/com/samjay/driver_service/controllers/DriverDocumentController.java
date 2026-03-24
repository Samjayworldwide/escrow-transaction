package com.samjay.driver_service.controllers;

import com.samjay.driver_service.dtos.requests.ConfirmUploadRequest;
import com.samjay.driver_service.dtos.responses.ApiResponse;
import com.samjay.driver_service.dtos.responses.UploadURLsResponse;
import com.samjay.driver_service.services.interfaces.DriverDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("NullableProblems")
@RestController
@RequestMapping("/api/driver/document")
@RequiredArgsConstructor
public class DriverDocumentController {

    private final DriverDocumentService driverDocumentService;

    @GetMapping("/sas-urls")
    public ResponseEntity<ApiResponse<UploadURLsResponse>> generateUploadUrls() {

        ApiResponse<UploadURLsResponse> response = driverDocumentService.generateUploadURLs();

        if (!response.isSuccessful())
            return ResponseEntity.badRequest().body(response);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm-upload")
    public ResponseEntity<ApiResponse<String>> confirmDocumentUpload(@Valid @RequestBody ConfirmUploadRequest confirmUploadRequest) {

        ApiResponse<String> response = driverDocumentService.confirmUpload(confirmUploadRequest);

        if (!response.isSuccessful())
            return ResponseEntity.badRequest().body(response);

        return ResponseEntity.ok(response);
    }
}
