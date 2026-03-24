package com.samjay.authentication_service.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UsernameRulesByRoleValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UsernameRulesByRole {

    String message() default "Username rules violation";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}