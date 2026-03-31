package com.samjay.driver_service.services.interfaces;

import com.samjay.driver_service.models.DriverLocation;

import java.util.List;
import java.util.UUID;

public interface RedisDriverLocationStore {

    void upsertDriverLocation(DriverLocation newLocation);

    List<DriverLocation> getDriversInCells(List<Long> cellIndexes);

    void removeDriver(UUID userId);
}
