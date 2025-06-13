package com.supos.common.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class UserNameConstraint implements ConstraintValidator<UserNameValidator, String> {

    public static final String REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9_\\-.@&+]*$";
    private static Pattern PATTERN = Pattern.compile(REGEX);

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        if (StringUtils.isNotBlank(name)) {
            if (name.length() < 3 || name.length() > 200 || !PATTERN.matcher(name).matches()) {
                return false;
            }
        }
        return true;
    }
}