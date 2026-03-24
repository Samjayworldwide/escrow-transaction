package com.samjay.driver_service.services.interfaces;

import com.samjay.driver_service.dtos.events.UserRegisteredEventDto;
import com.samjay.driver_service.dtos.requests.CompleteProfileRequest;
import com.samjay.driver_service.dtos.responses.ApiResponse;

public interface DriverService {

    void createDriver(UserRegisteredEventDto userRegisteredEventDto);

    ApiResponse<String> completeProfile(CompleteProfileRequest completeProfileRequest);
}
