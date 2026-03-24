package com.samjay.wallet_service.utility;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AppExtensions {

    private AppExtensions() {
    }

    public static final String CREDIT_WALLET_EVENT_TYPE = "CREDIT_WALLET";

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
