package com.supos.common.annotation;

import com.supos.common.enums.DataTimestampSrc;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 采集器数据时间戳来源校验
 * @date 2025/4/8 11:25
 */
public class DataTimestampSrcConstraint implements ConstraintValidator<DataTimestampSrcValidator, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        DataTimestampSrc src = DataTimestampSrc.get(value);
        return src != null;
    }
}
