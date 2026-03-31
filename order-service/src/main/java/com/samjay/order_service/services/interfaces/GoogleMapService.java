package com.samjay.order_service.services.interfaces;

import com.samjay.order_service.dtos.responses.ApiResponse;
import com.samjay.order_service.dtos.responses.DistanceAndDurationResponse;
import com.samjay.order_service.dtos.responses.LatitudeAndLongitudeResponse;

public interface GoogleMapService {

    ApiResponse<LatitudeAndLongitudeResponse> getLatitudeAndLongitudeFromAddress(String address);

    ApiResponse<DistanceAndDurationResponse> getDurationAndDistanceBetweenAddresses(String originAddress, String destinationAddress);
}
