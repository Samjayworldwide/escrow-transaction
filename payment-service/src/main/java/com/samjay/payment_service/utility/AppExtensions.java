package com.samjay.payment_service.utility;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AppExtensions {

    private AppExtensions() {
    }

    public static final String PAYMENT_INITIALIZATION_EVENT_TYPE = "PAYMENT_INITIALIZATION";

    public static final String PAYMENT_VERIFICATION_EVENT_TYPE = "PAYMENT_VERIFICATION";

    public static final String CLIENT_REQUEST_KEY_HEADER = "X-Client-Request-Key";

    public static final String USER_ID_CLAIM_KEY = "userId";

    public static final String USERNAME_CLAIM_KEY = "username";

    public static final String PAYMENT_INITIALIZATION_KAFKA_BINDING = "paymentInitialization-out-0";

    public static final String PAYMENT_VERIFICATION_KAFKA_BINDING = "paymentVerification-out-0";

    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public static <T> String serialize(T object) {

        try {

            if (object == null) return null;

            return objectMapper.writeValueAsString(object);

        } catch (Exception e) {

            throw new RuntimeException("Serialization failed", e);
        }
    }

    public static <T> T deserialize(String json, Class<T> type) {

        try {

            if (json == null || json.isEmpty()) return null;

            return objectMapper.readValue(json, type);

        } catch (Exception e) {

            throw new RuntimeException("Deserialization failed", e);
        }
    }

    public static String generateHash(String input) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) hex.append(String.format("%02x", b));

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
