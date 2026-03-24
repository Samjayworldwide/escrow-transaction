package com.samjay.customer_service.services.interfaces;

import com.samjay.customer_service.dtos.events.UserRegisteredEventDto;

public interface CustomerService {

    void createCustomer(UserRegisteredEventDto userRegisteredEventDto);
}
