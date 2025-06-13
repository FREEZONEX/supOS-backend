package com.supos.adpter.kong.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 网关别名校验
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConstraintOpenTypeValidator.class)
public @interface OpenTypeValidator {

    String message() default "menu.opentype.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
