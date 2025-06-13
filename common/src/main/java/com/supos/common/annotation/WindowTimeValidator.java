package com.supos.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = WindowTimeConstraint.class)
public @interface WindowTimeValidator {
    String message() default "uns.invalid.stream.time";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String intervalField() default "interval";

    String offsetField() default "offset";
}
