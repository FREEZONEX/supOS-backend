package com.supos.common.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ConstraintMenuNameValidator
 * @date 2025/4/17 16:26
 */
public class ConstraintMenuNameValidator implements ConstraintValidator<MenuNameValidator, String> {
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z_\\$]*$");

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(name)) {
            return true;
        }
        if (name.length() > 64 || !NAME_PATTERN.matcher(name).matches()) {
            return false;
        }
        return true;
    }
}
