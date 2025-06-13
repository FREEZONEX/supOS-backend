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
 * @description: 采集器网关类型校验
 * @date 2025/4/8 11:10
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CollectorGatewayTypeConstraint.class)
public @interface CollectorGatewayTypeValidator {

    String message() default "collector.gateway.type.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
