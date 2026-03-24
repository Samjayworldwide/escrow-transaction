package com.samjay.driver_service.utility;

import com.samjay.driver_service.entities.Driver;
import com.samjay.driver_service.entities.DriverDocument;
import com.samjay.driver_service.enumerations.DocumentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class AppExtensions {

    private AppExtensions() {
    }

    public static final String USER_ID_CLAIM_KEY = "userId";

    public static final String USERNAME_CLAIM_KEY = "username";

    private static final List<String> TRACKED_FIELDS = Arrays.asList(
            "phoneNumber",
            "profilePictureUrl",
            "licensePlateNumber",
            "identifiationNumber"

    );

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
}
