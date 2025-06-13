package com.supos.common.annotation;

import com.supos.common.utils.DateTimeUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;

import static com.supos.common.utils.ConstraintErrTipUtils.setErrMsg;

public class DateTimeConstraint implements ConstraintValidator<DateTimeValidator, String> {

    DateTimeValidator validator;

    @Override
    public void initialize(DateTimeValidator constraintAnnotation) {
        validator = constraintAnnotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.length() == 0) {
            return true;
        }
        boolean ok = false;
        if (value.length() >= 4) {
            value = value.trim();
            ok = parseDate(value) != null;
        }
        if (!ok) {
            setErrMsg(context, validator.field(), "uns.stream.invalid.datetime", value);
        }
        return ok;
    }


    public static String parseDate(String value) {
        Instant instant = DateTimeUtils.parseDate(value);
        return instant != null ? instant.toString() : null;
    }
}