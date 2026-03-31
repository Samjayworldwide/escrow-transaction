package com.samjay.driver_service.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriverLocation {

    private UUID userId;

    private double latitude;

    private double longitude;

    private long h3Index;

    private long lastUpdated;
}
