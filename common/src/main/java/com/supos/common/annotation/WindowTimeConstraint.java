package com.supos.common.annotation;

import com.supos.common.enums.TimeUnits;
import com.supos.common.utils.IntegerUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import static com.supos.common.utils.ConstraintErrTipUtils.setErrMsg;

public class WindowTimeConstraint implements ConstraintValidator<WindowTimeValidator, WindowIntervalOffset> {
    WindowTimeValidator validator;

    @Override
    public void initialize(WindowTimeValidator constraintAnnotation) {
        validator = constraintAnnotation;
    }

    @Override
    public boolean isValid(WindowIntervalOffset window, ConstraintValidatorContext context) {
        if (window == null) {
            return true;
        }
        if (StringUtils.isEmpty(window.getInterval())) {
            setErrMsg(context, validator.intervalField(), "uns.window.interval.empty");
            return false;
        }
        Long interval = parseTime(window.getInterval());
        if (interval == null) {
            setErrMsg(context, validator.intervalField(), "uns.invalid.stream.time", window.getInterval());
            return false;
        }


        Long offset = parseTime(window.getOffset());
        if (offset != null && offset >= interval) {
            setErrMsg(context, validator.offsetField(), "uns.invalid.stream.window.intervalGtOffset");
            return false;
        }
        long intervalMills = interval / 1_000_000;
        if (intervalMills < 1000) {
            setErrMsg(context, validator.offsetField(), "uns.window.interval.tooSmall");
            return false;
        }
        window.setIntervalMills(intervalMills);
        return true;
    }

    private static Long parseTime(String value) {
        if (value != null && value.length() >= 2) {
            value = value.trim();
            Integer timeNum = IntegerUtils.parseInt(value.substring(0, value.length() - 1).trim());
            if (timeNum != null) {
                char unit = value.charAt(value.length() - 1);
                TimeUnits timeUnits = TimeUnits.of(unit);
                if (timeUnits != null) {
                    return timeUnits.toNanoSecond(timeNum);
                }
            }
        }
        return null;
    }
}