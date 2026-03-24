package com.samjay.customer_service.services.implementations;

import com.samjay.customer_service.dtos.events.UserRegisteredEventDto;
import com.samjay.customer_service.entities.Customer;
import com.samjay.customer_service.repositories.CustomerRepository;
import com.samjay.customer_service.services.interfaces.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImplementation implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public void createCustomer(UserRegisteredEventDto userRegisteredEventDto) {

        try {

            if (userRegisteredEventDto == null || userRegisteredEventDto.userId() == null) {

                log.warn("Received null UserRegisteredRecordDto. Skipping customer creation.");

                return;
            }

            boolean customerExists = customerRepository.existsByUserId(userRegisteredEventDto.userId());

            if (customerExists) {

                log.info("Customer with userId {} already exists. Skipping creation.", userRegisteredEventDto.userId());

                return;
            }

            Customer customer = Customer
                    .builder()
                    .userId(userRegisteredEventDto.userId())
                    .firstname(userRegisteredEventDto.firstname())
                    .lastname(userRegisteredEventDto.lastname())
                    .email(userRegisteredEventDto.email())
                    .username(userRegisteredEventDto.username())
                    .build();

            customerRepository.save(customer);

        } catch (Exception e) {

            log.error("Error occurred while creating customer with user ID {} exception: {}", userRegisteredEventDto != null
                    ? userRegisteredEventDto.userId() : null, e.getMessage(), e);

            throw e;

        }
    }
}
