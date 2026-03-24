package com.samjay.customer_service.functions;

import com.samjay.customer_service.dtos.events.UserRegisteredEventDto;
import com.samjay.customer_service.enumerations.Roles;
import com.samjay.customer_service.services.interfaces.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CustomerFunction {

    private final CustomerService customerService;

    @Bean
    public Consumer<UserRegisteredEventDto> createCustomer() {

        return userRegisteredEventDto -> {

            if (userRegisteredEventDto == null || userRegisteredEventDto.role() != Roles.CUSTOMER)
                return;

            try {

                log.info("Received User registration event to create customer profile for user with user ID: {}", userRegisteredEventDto.userId());

                customerService.createCustomer(userRegisteredEventDto);

                log.info("Sucessfully created customer profile for user with userId: {}", userRegisteredEventDto.userId());

            } catch (Exception e) {

                log.error("Error processing UserRegisteredRecordDto: {}. Exception: {}", userRegisteredEventDto, e.getMessage(), e);

                throw e;
            }
        };
    }
}
