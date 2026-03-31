package com.samjay.driver_service.services.interfaces;

import com.samjay.driver_service.dtos.responses.SearchDriverH3Response;

import java.util.UUID;

public interface H3DriverMatchingService {

    void updateDriverLocation(UUID userId, double latitude, double longitude);

    SearchDriverH3Response findNearestDrivers(double latitude, double longitude);
}
