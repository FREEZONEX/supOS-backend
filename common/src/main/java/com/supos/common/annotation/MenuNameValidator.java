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
 * @description: MenuNameValidator
 * @date 2025/4/17 16:26
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConstraintMenuNameValidator.class)
public @interface MenuNameValidator {

    String message() default "menu.name.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
