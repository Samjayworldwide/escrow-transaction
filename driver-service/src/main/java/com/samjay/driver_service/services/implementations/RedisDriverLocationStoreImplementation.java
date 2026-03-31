package com.samjay.driver_service.services.implementations;

import com.samjay.driver_service.exceptions.ApplicationException;
import com.samjay.driver_service.models.DriverLocation;
import com.samjay.driver_service.services.interfaces.RedisDriverLocationStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import redis.clients.jedis.UnifiedJedis;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.samjay.driver_service.utility.AppExtensions.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisDriverLocationStoreImplementation implements RedisDriverLocationStore {

    private final UnifiedJedis jedis;

    private final ObjectMapper objectMapper;

    @Override
    public void upsertDriverLocation(DriverLocation newLocation) {

        String driverKey = DRIVER_LOCATION_PREFIX + newLocation.getUserId().toString();

        // Check if driver already exists so we can clean up their old H3 cell
        String existingJson = jedis.get(driverKey);

        if (existingJson != null) {

            try {

                DriverLocation existing = objectMapper.readValue(existingJson, DriverLocation.class);

                // Remove driver from their previous cell's set
                jedis.srem(H3_CELL_PREFIX + existing.getH3Index(), newLocation.getUserId().toString());

                log.debug("Removed {} from old H3 cell {}", newLocation.getUserId(), existing.getH3Index());

            } catch (JacksonException e) {

                log.warn("Could not parse existing driver location for {}", newLocation.getUserId());

            }
        }

        try {

            // 1. Save full driver details (with TTL so stale drivers auto-expire)
            String locationJson = objectMapper.writeValueAsString(newLocation);

            jedis.setex(driverKey, DRIVER_TTL_SECONDS, locationJson);

            // 2. Add driver email to their new H3 cell set
            String cellKey = H3_CELL_PREFIX + newLocation.getH3Index();

            jedis.sadd(cellKey, newLocation.getUserId().toString());

            jedis.expire(cellKey, DRIVER_TTL_SECONDS); // Cell set also expires if unused

            log.debug("Upserted driver {} into H3 cell {}", newLocation.getUserId(), newLocation.getH3Index());

        } catch (JacksonException e) {

            log.error("Failed to serialize driver location for {}", newLocation.getUserId(), e);

            throw new ApplicationException("Failed to serialize driver location", HttpStatus.BAD_REQUEST);

        }
    }

    @Override
    public List<DriverLocation> getDriversInCells(List<Long> cellIndexes) {

        List<DriverLocation> result = new ArrayList<>();

        for (Long cellIndex : cellIndexes) {

            // Get all driver userIds in this cell
            Set<String> userIds = jedis.smembers(H3_CELL_PREFIX + cellIndex);

            for (String userId : userIds) {

                String json = jedis.get(DRIVER_LOCATION_PREFIX + userId);

                if (json != null) {

                    // Could be null if TTL expired between the two calls

                    try {

                        result.add(objectMapper.readValue(json, DriverLocation.class));

                    } catch (JacksonException e) {

                        log.warn("Could not deserialize driver location for {}", userId);

                    }
                }
            }
        }

        return result;

    }

    @Override
    public void removeDriver(UUID userId) {

        String driverKey = DRIVER_LOCATION_PREFIX + userId;

        String existingJson = jedis.get(driverKey);

        if (existingJson != null) {

            try {

                DriverLocation existing = objectMapper.readValue(existingJson, DriverLocation.class);

                jedis.srem(H3_CELL_PREFIX + existing.getH3Index(), userId.toString());

            } catch (JacksonException e) {

                log.warn("Could not clean up H3 cell for driver {}", userId);

            }

            jedis.del(driverKey);

        }
    }
}
