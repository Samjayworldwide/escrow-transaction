package com.samjay.driver_service.configurations;

import com.uber.h3core.H3Core;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class UberH3Configuration {

    @Bean
    public H3Core h3Core() throws IOException {

        return H3Core.newInstance();

    }
}
