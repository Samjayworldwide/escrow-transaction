package com.samjay.driver_service.services.interfaces;

import com.samjay.driver_service.dtos.events.DriverSearchEventDto;
import com.samjay.driver_service.dtos.events.UserRegisteredEventDto;
import com.samjay.driver_service.dtos.requests.CompleteProfileRequest;
import com.samjay.driver_service.dtos.responses.ApiResponse;
import com.samjay.driver_service.models.DriverLocation;

import java.util.List;
import java.util.UUID;

public interface DriverService {

    void createDriver(UserRegisteredEventDto userRegisteredEventDto);

    ApiResponse<String> completeProfile(CompleteProfileRequest completeProfileRequest);

    void searchForDriverClosestToSeller(DriverSearchEventDto driverSearchEventDto);

    ApiResponse<String> acceptDeliveryRequest(String clientRequestKey, UUID orderId);

    ApiResponse<List<DriverLocation>> searchForNearbyDrivers(UUID orderId);

}
