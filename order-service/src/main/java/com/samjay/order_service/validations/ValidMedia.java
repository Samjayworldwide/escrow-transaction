package com.samjay.order_service.validations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MediaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMedia {

    String message() default "Invalid item media file.";

    long maxSizeInMB() default 50;

    String[] allowedContentTypes() default {
            "image/jpeg",
            "image/png",
            "image/webp",
            "video/mp4",
            "video/quicktime"
    };

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
