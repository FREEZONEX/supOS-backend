package com.supos.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ServiceNameValidator
 * @date 2025/4/17 16:26
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConstraintServiceNameValidator.class)
public @interface ServiceNameValidator {

    String message() default "menu.servicename.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
