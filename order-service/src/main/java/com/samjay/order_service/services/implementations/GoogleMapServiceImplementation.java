package com.samjay.order_service.services.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samjay.order_service.dtos.responses.ApiResponse;
import com.samjay.order_service.dtos.responses.DistanceAndDurationResponse;
import com.samjay.order_service.dtos.responses.LatitudeAndLongitudeResponse;
import com.samjay.order_service.exceptions.ApplicationException;
import com.samjay.order_service.services.interfaces.GoogleMapService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("NullableProblems")
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleMapServiceImplementation implements GoogleMapService {

    @Value("${google.api.key}")
    private String googleMapsApiKey;

    @Value("${google.base-url}")
    private String googleMapsBaseUrl;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    @CircuitBreaker(name = "interServiceCircuit", fallbackMethod = "fallbackGetLatitudeAndLongitudeFromAddress")
    @Retry(name = "interServiceRetry", fallbackMethod = "fallbackGetLatitudeAndLongitudeFromAddress")
    @Bulkhead(name = "interServiceBulkhead", fallbackMethod = "fallbackGetLatitudeAndLongitudeFromAddress", type = Bulkhead.Type.SEMAPHORE)
    @Override
    public ApiResponse<LatitudeAndLongitudeResponse> getLatitudeAndLongitudeFromAddress(String address) {

        try {

            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);

            String url = googleMapsBaseUrl + "geocode/json?address=" + encodedAddress + "&key=" + googleMapsApiKey;

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {

                log.error("Failed to get coordinates. HTTP Status: {}", response.getStatusCode());

                throw new ApplicationException("Failed to get geo-coordinates. Please try again later.", HttpStatus.BAD_REQUEST);
            }

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            if (!jsonNode.path("status").asText().equals("OK")) {

                log.info("Geocoding failed for address: {}, API response status: {}", address, jsonNode.path("status").asText());

                throw new ApplicationException("Geocoding failed: " + jsonNode.path("status").asText(), HttpStatus.BAD_REQUEST);
            }

            JsonNode results = jsonNode.path("results");

            if (results.isEmpty()) {

                log.info("No results found for the provided address: {}", address);

                throw new ApplicationException("No results found for the provided address.", HttpStatus.BAD_REQUEST);
            }

            JsonNode location = results
                    .get(0)
                    .path("geometry")
                    .path("location");

            double latitude = location.path("lat").asDouble();

            double longitude = location.path("lng").asDouble();

            LatitudeAndLongitudeResponse latLngResponse = new LatitudeAndLongitudeResponse(latitude, longitude);

            return ApiResponse.success("Latitude and longitude fetched successfully.", latLngResponse);

        } catch (Exception e) {

            log.error("Error while fetching latitude and longitude from Google Maps API, exception message: {}", e.getMessage(), e);

            throw new ApplicationException("Failed to fetch latitude and longitude. Please try again later.", HttpStatus.BAD_REQUEST);
        }
    }

    @CircuitBreaker(name = "interServiceCircuit", fallbackMethod = "fallbackGetDurationAndDistanceBetweenAddresses")
    @Retry(name = "interServiceRetry", fallbackMethod = "fallbackGetDurationAndDistanceBetweenAddresses")
    @Bulkhead(name = "interServiceBulkhead", fallbackMethod = "fallbackGetDurationAndDistanceBetweenAddresses", type = Bulkhead.Type.SEMAPHORE)
    @Override
    public ApiResponse<DistanceAndDurationResponse> getDurationAndDistanceBetweenAddresses(String originAddress, String destinationAddress) {

        try {

            String encodedOrigin = URLEncoder.encode(originAddress, StandardCharsets.UTF_8);

            String encodedDestination = URLEncoder.encode(destinationAddress, StandardCharsets.UTF_8);

            String url = googleMapsBaseUrl + "distancematrix/json?origins=" + encodedOrigin + "&destinations=" + encodedDestination + "&key=" + googleMapsApiKey;

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {

                log.error("Failed to get distance and duration, Reason: HTTP Status: {}", response.getStatusCode());

                throw new ApplicationException("Failed to get duration and distance. Please try again later.", HttpStatus.BAD_REQUEST);
            }

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            if (!jsonNode.path("status").asText().equals("OK")) {

                log.info("Status for origin address: {}, and destintion address: {} was not equal to OK, Status: {}",
                        encodedOrigin,
                        encodedDestination,
                        jsonNode.path("status").asText()
                );

                throw new ApplicationException("Duration and distance failed: " + jsonNode.path("status").asText(), HttpStatus.BAD_REQUEST);
            }

            JsonNode element = jsonNode
                    .path("rows")
                    .get(0)
                    .path("elements")
                    .get(0);

            if (!element.path("status").asText().equals("OK")) {

                log.info("Route calculation failed for origin address: {}, and destintion address: {}, Status: {}",
                        encodedOrigin,
                        encodedDestination,
                        element.path("status").asText()
                );

                throw new ApplicationException("Route calculation failed: " + element.path("status").asText(), HttpStatus.BAD_REQUEST);
            }

            long distanceMeters = element
                    .path("distance")
                    .path("value")
                    .asLong();

            String duration = element
                    .path("duration")
                    .path("text")
                    .asText();

            DistanceAndDurationResponse distanceAndDurationResponse = new DistanceAndDurationResponse(
                    Math.round(distanceMeters / 1000.0),
                    duration
            );

            return ApiResponse.success("Duration and distance fetched successfully.", distanceAndDurationResponse);

        } catch (Exception e) {

            log.error("Error while fetching duration and distance from Google Maps API, exception message: {}", e.getMessage(), e);

            throw new ApplicationException("Failed to fetch duration and distance. Please try again later.", HttpStatus.BAD_REQUEST);
        }
    }

    public ApiResponse<LatitudeAndLongitudeResponse> fallbackGetLatitudeAndLongitudeFromAddress(String address, Throwable ex) {

        log.error("Google Maps API is currently unavailable. Falling back to default coordinates. Exception message: {}", ex.getMessage(), ex);

        return ApiResponse.error("Google Maps API is currently unavailable. Returning default coordinates.");

    }

    public ApiResponse<DistanceAndDurationResponse> fallbackGetDurationAndDistanceBetweenAddresses(String originAddress,
                                                                                                   String destinationAddress,
                                                                                                   Throwable ex) {

        log.error("Google Maps API is currently unavailable. Falling back to default distance and duration. Exception message: {}", ex.getMessage(), ex);

        return ApiResponse.error("Google Maps API is currently unavailable. Returning default distance and duration.");

    }
}
