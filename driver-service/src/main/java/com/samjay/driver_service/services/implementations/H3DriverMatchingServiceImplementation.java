package com.samjay.driver_service.services.implementations;

import com.samjay.driver_service.dtos.responses.SearchDriverH3Response;
import com.samjay.driver_service.models.DriverLocation;
import com.samjay.driver_service.services.interfaces.H3DriverMatchingService;
import com.samjay.driver_service.services.interfaces.RedisDriverLocationStore;
import com.uber.h3core.H3Core;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.samjay.driver_service.utility.AppExtensions.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class H3DriverMatchingServiceImplementation implements H3DriverMatchingService {

    private final RedisDriverLocationStore redisDriverLocationStore;

    private final H3Core h3;

    @Override
    public void updateDriverLocation(UUID userId, double latitude, double longitude) {

        long cellIndex = h3.latLngToCell(latitude, longitude, H3_RESOLUTION);

        DriverLocation location = new DriverLocation(
                userId,
                latitude,
                longitude,
                cellIndex,
                System.currentTimeMillis()
        );

        redisDriverLocationStore.upsertDriverLocation(location);

        log.info("Driver with userId {} is now in H3 cell {} (res {})", userId, cellIndex, H3_RESOLUTION);
    }

    @Override
    public SearchDriverH3Response findNearestDrivers(double latitude, double longitude) {

        long h3Cell = h3.latLngToCell(latitude, longitude, H3_RESOLUTION);

        log.info("Searching from H3 cell {}", h3Cell);

        for (int ring = 0; ring <= MAX_RINGS; ring++) {

            List<Long> searchArea = h3.gridDisk(h3Cell, ring);

            List<DriverLocation> drivers = redisDriverLocationStore.getDriversInCells(searchArea);

            if (!drivers.isEmpty()) {

                double approxRadius = ring * KM_PER_RING_RES_8;

                log.info("Found {} driver(s) within ring {} (~{}km)", drivers.size(), ring, approxRadius);

                return new SearchDriverH3Response(ring, approxRadius, drivers.size(), drivers);
            }
        }

        log.warn("No drivers found within {} rings (~{}km) of user", MAX_RINGS, MAX_RINGS * KM_PER_RING_RES_8);

        return new SearchDriverH3Response(MAX_RINGS, MAX_RINGS * KM_PER_RING_RES_8, 0, List.of());
    }
}
