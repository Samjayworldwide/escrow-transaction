package com.samjay.notification_service.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samjay.notification_service.models.CursorPayload;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AppExtensions {

    private AppExtensions() {
    }

    public static final String USER_ID_CLAIM_KEY = "userId";

    public static final String USERNAME_CLAIM_KEY = "username";

    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public static <T> String serialize(T object) {

        try {

            if (object == null) return null;

            return objectMapper.writeValueAsString(object);

        } catch (Exception e) {

            throw new RuntimeException("Serialization failed", e);
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

    public static String encodeCursor(CursorPayload payload) {

        try {

            String json = objectMapper.writeValueAsString(payload);

            return Base64.getEncoder().encodeToString(json.getBytes());

        } catch (Exception e) {

            throw new RuntimeException("Failed to encode cursor", e);

        }
    }

    public static CursorPayload decodeCursor(String cursor) {

        if (cursor == null || cursor.isBlank()) return null;

        try {

            byte[] bytes = Base64.getDecoder().decode(cursor);

            String json = new String(bytes);

            return objectMapper.readValue(json, CursorPayload.class);

        } catch (Exception e) {

            return null;

        }
    }
}
