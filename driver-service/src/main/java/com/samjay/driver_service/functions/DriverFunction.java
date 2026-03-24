package com.samjay.driver_service.functions;

import com.samjay.driver_service.dtos.events.UserRegisteredEventDto;
import com.samjay.driver_service.enumerations.Roles;
import com.samjay.driver_service.services.interfaces.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DriverFunction {

    private final DriverService driverService;

    @Bean
    public Consumer<UserRegisteredEventDto> createDriver() {

        return userRegisteredEventDto -> {

            if (userRegisteredEventDto == null || userRegisteredEventDto.role() != Roles.DRIVER)
                return;

            try {

                log.info("Received UserRegistered event to create a driver for user with user ID with: {}", userRegisteredEventDto.userId());

                driverService.createDriver(userRegisteredEventDto);

                log.info("Successfully created driver for user with user ID: {}", userRegisteredEventDto.userId());

            } catch (Exception e) {

                log.error("Error creating driver for user with user ID: {}. Error message: {}", userRegisteredEventDto.userId(), e.getMessage());

                throw e;
            }
        };
    }
}
