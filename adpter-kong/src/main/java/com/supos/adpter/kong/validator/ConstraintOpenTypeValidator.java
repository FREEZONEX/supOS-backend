package com.supos.adpter.kong.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;

public class ConstraintOpenTypeValidator implements ConstraintValidator<OpenTypeValidator, Integer> {

    private static List<Integer> OPEN_TYPE_LIST = Arrays.asList(0, 1);

    @Override
    public boolean isValid(Integer type, ConstraintValidatorContext context) {
        if (type == null || !OPEN_TYPE_LIST.contains(type)) {
            return false;
        }
        return true;
    }
}
