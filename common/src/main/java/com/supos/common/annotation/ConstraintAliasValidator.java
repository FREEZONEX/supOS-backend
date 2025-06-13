package com.supos.common.annotation;

import com.supos.common.Constants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

import static com.supos.common.utils.ConstraintErrTipUtils.setErrMsg;

public class ConstraintAliasValidator implements ConstraintValidator<AliasValidator, String> {

    private static final Pattern ALIAS_PATTERN = Pattern.compile(Constants.ALIAS_REG);

    @Override
    public boolean isValid(String alias, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(alias)) {
            return true;
        }
        if (alias.length() > 63) {
            setErrMsg(context, "alias", "uns.alias.length.limit.exceed", "63");
            return false;
        }
        if (!ALIAS_PATTERN.matcher(alias).matches()) {
            setErrMsg(context, "alias", "uns.invalid.alias", "63");
            return false;
        }
        return true;
    }
}
