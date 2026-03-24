package com.samjay.customer_service.utility;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AppExtensions {

    private AppExtensions(){}

    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public static <T> String serialize(T object) {

        try {

            if (object == null) return null;

            return objectMapper.writeValueAsString(object);

        } catch (Exception e) {

            throw new RuntimeException("Serialization failed", e);
        }
    }
}
