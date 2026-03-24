package com.samjay.order_service.validations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class MediaValidator implements ConstraintValidator<ValidMedia, MultipartFile> {

    private long maxSizeInBytes;

    private Set<String> allowedContentTypes;

    @Override
    public void initialize(ValidMedia constraintAnnotation) {

        this.maxSizeInBytes = constraintAnnotation.maxSizeInMB() * 1024 * 1024;

        this.allowedContentTypes = Arrays.stream(constraintAnnotation.allowedContentTypes())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(MultipartFile multipartFile, ConstraintValidatorContext constraintValidatorContext) {

        constraintValidatorContext.disableDefaultConstraintViolation();

        if (multipartFile == null || multipartFile.isEmpty()) {

            constraintValidatorContext.buildConstraintViolationWithTemplate("Item media file cannot be empty.").addConstraintViolation();

            return false;
        }

        if (multipartFile.getSize() > maxSizeInBytes) {

            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("File size exceeds the " + (maxSizeInBytes / (1024 * 1024)) + "MB limit.")
                    .addConstraintViolation();

            return false;
        }

        if (!allowedContentTypes.contains(multipartFile.getContentType())) {

            constraintValidatorContext.buildConstraintViolationWithTemplate("Unsupported media type: " + multipartFile.getContentType() +
                    ". Allowed types are: " + String.join(", ", allowedContentTypes)).addConstraintViolation();

            return false;
        }

        return true;
    }
}
