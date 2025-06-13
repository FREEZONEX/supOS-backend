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
 * @description: UserNameValidator
 * @date 2025/4/25 14:21
 */
@Target({ElementType.FIELD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserNameConstraint.class)
public @interface UserNameValidator {

    String message() default "user.username.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
