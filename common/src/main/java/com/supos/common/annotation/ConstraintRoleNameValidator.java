package com.supos.common.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ConstraintRoleNameValidator
 * @date 2025/4/22 10:40
 */
public class ConstraintRoleNameValidator implements ConstraintValidator<RoleNameValidator, String> {

    public static final Pattern PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z0-9]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value.length() > 0 || !PATTERN.matcher(value).matches() || value.startsWith("deny-")) {
            return false;
        }
        return false;
    }
}
