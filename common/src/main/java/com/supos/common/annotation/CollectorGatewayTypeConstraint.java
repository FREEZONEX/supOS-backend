package com.supos.common.annotation;

import com.supos.common.enums.CollectorGatewayType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 采集器网关类型校验
 * @date 2025/4/8 11:11
 */
public class CollectorGatewayTypeConstraint implements ConstraintValidator<CollectorGatewayTypeValidator, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        CollectorGatewayType gatewayType = CollectorGatewayType.get(value);
        return gatewayType != null;
    }
}
