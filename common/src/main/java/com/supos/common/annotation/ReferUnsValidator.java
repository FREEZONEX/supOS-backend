package com.supos.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReferUnsConstraint.class)
public @interface ReferUnsValidator {
    String message() default "uns.refer.idAndAliasEmpty";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
