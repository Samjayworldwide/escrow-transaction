package com.samjay.driver_service.utility;

import com.samjay.driver_service.entities.Driver;
import com.samjay.driver_service.entities.DriverDocument;
import com.samjay.driver_service.enumerations.DocumentType;
import com.samjay.driver_service.models.CursorPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;

@Slf4j
public class AppExtensions {

    private AppExtensions() {
    }

    public static final String USER_ID_CLAIM_KEY = "userId";

    public static final String USERNAME_CLAIM_KEY = "username";

    public static final String DRIVER_LOCATION_PREFIX = "driver:location:";

    public static final String H3_CELL_PREFIX = "h3:cell:";

    public static final int DRIVER_TTL_SECONDS = 300;

    public static final int H3_RESOLUTION = 8;

    public static final int MAX_RINGS = 10;

    public static final double KM_PER_RING_RES_8 = 1.2;

    public static final String BEARER_PREFIX = "Bearer ";

    public static final String AUTH_ATTRIBUTE = "AUTHENTICATED_USER";

    public static final String EMPTY_DRIVER_SEARCH_RESULT_EVENT_TYPE = "EMPTY_DRIVER_SEARCH_RESULT";

    public static final String EMPTY_DRIVER_SEARCH_RESULT_KAFKA_BINDING = "emptyDriverSearchResult-out-0";

    public static final String ORDER_DELIVERY_NOTIFICATION_EVENT_TYPE = "ORDER_DELIVERY_NOTIFICATION";

    public static final String ORDER_DELIVERY_NOTIFICATION_KAFKA_BINDING = "orderDeliveryNotification-out-0";

    public static final String ORDER_DRIVER_ASSIGNMENT_EVENT_TYPE = "ORDER_DRIVER_ASSIGNMENT";

    public static final String NOTIFICATION_DELIVERY_ACCEPTANCE_KAFKA_BINDING = "notificationDeliveryAccepted-out-0";

    public static final String EMAIL_DELIVERY_ACCEPTANCE_KAFKA_BINDING = "emailDeliveryAccepted-out-0";

    public static final String ORDER_DELIVERY_UPDATE_EVENT_TYPE = "ORDER_DELIVERY_UPDATE";

    public static final String ORDER_DELIVERY_UPDATE_KAFKA_BINDING = "orderDeliveryUpdate-out-0";

    public static final String ORDER_TRACKING_STAGE_UPDATE_EVENT_TYPE = "ORDER_TRACKING_STAGE_UPDATE";

    public static final String ORDER_TRACKING_STAGE_UPDATE_KAFKA_BINDING = "orderTrackingStageUpdate-out-0";

    public static final String TRACKING_STAGE_NOTIFICATION_EVENT_TYPE = "TRACKING_STAGE_NOTIFICATION";

    public static final String TRACKING_STAGE_NOTIFICATION_KAFKA_BINDING = "trackingStageNotification-out-0";

    public static final String DELIVERY_COMPLETED_EVENT_TYPE = "DELIVERY_COMPLETED";

    public static final String DELIVERY_COMPLETED_KAFKA_BINDING = "deliveryCompleted-out-0";

    public static final String CLIENT_REQUEST_KEY_HEADER = "X-Client-Request-Key";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> TRACKED_FIELDS = Arrays.asList(
            "phoneNumber",
            "profilePictureUrl",
            "licensePlateNumber",
            "identificationNumber"
    );

    public static String generateVerificationCode() {

        Random random = new Random();

        int randomVerificationCodeNumber = random.nextInt(999999);

        String verificationCode = Integer.toString(randomVerificationCodeNumber);

        while (verificationCode.length() < 6) {

            verificationCode = "0".concat(verificationCode);
        }

        return verificationCode;
    }

    private static final List<DocumentType> REQUIRED_DOCUMENTS = List.of(
            DocumentType.DRIVERS_LICENSE,
            DocumentType.VEHICLE_REGISTRATION,
            DocumentType.NATIONAL_ID
    );

    @SuppressWarnings("java:S3011")
    public static double calculateCompletion(Driver driver) {

        long totalFields = TRACKED_FIELDS.size();

        long filledFields = 0;

        for (String fieldName : TRACKED_FIELDS) {

            try {

                Field field = Driver.class.getDeclaredField(fieldName);

                field.setAccessible(true);

                Object value = field.get(driver);

                if (value != null) {

                    if (value instanceof String stringValue) {

                        if (StringUtils.hasText(stringValue)) {

                            filledFields++;

                        }

                    } else {

                        filledFields++;

                    }
                }

            } catch (Exception exception) {

                log.warn("Profile completion error: {}", exception.getMessage());

            }
        }

        long totalDocuments = REQUIRED_DOCUMENTS.size();

        long uploadedDocuments = driver.getDocuments().stream()
                .map(DriverDocument::getDocumentType)
                .distinct()
                .filter(REQUIRED_DOCUMENTS::contains)
                .count();

        long totalItems = totalFields + totalDocuments;

        long completedItems = filledFields + uploadedDocuments;

        return Math.round(((double) completedItems / totalItems) * 100);

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
