package com.samjay.authentication_service.annotations;

import com.samjay.authentication_service.dtos.requests.UserRegistrationRequest;
import com.samjay.authentication_service.enumerations.Roles;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UsernameRulesByRoleValidator
        implements ConstraintValidator<UsernameRulesByRole, UserRegistrationRequest> {

    @Override
    public boolean isValid(UserRegistrationRequest request, ConstraintValidatorContext context) {

        if (request == null)
            return true;

        Roles role = request.getRole();

        String username = request.getUsername();

        if (role == null)
            return true;

        context.disableDefaultConstraintViolation();

        if (role == Roles.CUSTOMER) {

            if (username == null || username.trim().isEmpty()) {

                context.buildConstraintViolationWithTemplate("Username is required for CUSTOMER")
                        .addPropertyNode("username")
                        .addConstraintViolation();

                return false;
            }

            if (username.length() < 3 || username.length() > 30) {

                context.buildConstraintViolationWithTemplate("Username must be between 3 and 30 characters")
                        .addPropertyNode("username")
                        .addConstraintViolation();

                return false;
            }

            return true;
        } else {

            if (username != null && !username.trim().isEmpty()) {

                context.buildConstraintViolationWithTemplate("Username must not be provided for " + role)
                        .addPropertyNode("username")
                        .addConstraintViolation();

                return false;
            }

            return true;
        }
    }
}