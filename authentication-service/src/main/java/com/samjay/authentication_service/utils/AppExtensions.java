package com.samjay.authentication_service.utils;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Random;

public class AppExtensions {

    private AppExtensions() {
    }

    public static final String EMAIL_VERIFICATION_KAFKA_BINDING = "sendVerificationCode-out-0";

    public static final String EMAIL_VERIFICATION_EVENT_TYPE = "EMAIL_VERIFICATION";

    public static final String USER_REGISTERED_KAFKA_BINDING = "userRegistered-out-0";

    public static final String USER_REGISTERED_EVENT_TYPE = "USER_REGISTERED";

    public static final String CLIENT_REQUEST_KEY_HEADER = "X-Client-Request-Key";

    public static final int MAX_LOGIN_ATTEMPTS = 5;

    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public static String generateVerificationCode() {

        Random random = new Random();

        int randomVerificationCodeNumber = random.nextInt(999999);

        String verificationCode = Integer.toString(randomVerificationCodeNumber);

        while (verificationCode.length() < 6) {

            verificationCode = "0".concat(verificationCode);
        }

        return verificationCode;
    }

    public static LocalDateTime getCurrentDateTime() {

        return LocalDateTime.now();
    }

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
