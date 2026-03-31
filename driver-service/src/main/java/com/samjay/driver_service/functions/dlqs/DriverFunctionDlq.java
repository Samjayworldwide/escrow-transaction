package com.samjay.driver_service.functions.dlqs;

import com.samjay.driver_service.dtos.events.DriverSearchEventDto;
import com.samjay.driver_service.dtos.events.UserRegisteredEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class DriverFunctionDlq {

    /*
    This function will be triggered when a message fails to process in the main function and is sent to the DLQ.
    You can enhance this function by saving the failed record to a database or triggering an alerting mechanism for manual intervention.
     */

    @Bean
    public Consumer<UserRegisteredEventDto> createDriverDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for user: {}", failedRecord.userId());
    }

    @Bean
    public Consumer<DriverSearchEventDto> driverSearchDlq() {

        return failedRecord -> log.error("Message landed in DLQ — manual intervention required for order with reference number: {}",
                failedRecord.orderReferenceNumber());
    }
}
