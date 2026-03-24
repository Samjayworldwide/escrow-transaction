package com.samjay.driver_service.services.interfaces;

import com.samjay.driver_service.dtos.requests.ConfirmUploadRequest;
import com.samjay.driver_service.dtos.responses.ApiResponse;
import com.samjay.driver_service.dtos.responses.UploadURLsResponse;

public interface DriverDocumentService {

    ApiResponse<UploadURLsResponse> generateUploadURLs();

    ApiResponse<String> confirmUpload(ConfirmUploadRequest confirmUploadRequest);
}
