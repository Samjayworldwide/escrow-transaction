package com.samjay.driver_service.dtos.responses;

import com.samjay.driver_service.models.DriverLocation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchDriverH3Response {

    private int ringSearched; // How many H3 rings expanded before finding drivers

    private double approxRadiusKm; // Human-readable: ringSearched * ~1.2km per ring at res 8

    private int totalDriversFound;

    private List<DriverLocation> drivers;
}