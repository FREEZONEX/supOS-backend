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
 * @description: 采集器数据时间戳来源校验
 * @date 2025/4/8 11:25
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DataTimestampSrcConstraint.class)
public @interface DataTimestampSrcValidator {

    String message() default "collector.gateway.data.timestamp.src.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
