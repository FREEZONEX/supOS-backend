package com.supos.common.annotation;

import com.supos.common.dto.InstanceField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class ReferUnsConstraint implements ConstraintValidator<ReferUnsValidator, InstanceField> {
    final ConstraintAliasValidator aliasValidator = new ConstraintAliasValidator();

    @Override
    public boolean isValid(InstanceField uns, ConstraintValidatorContext context) {
        String alias = uns.getAlias();
        if (!StringUtils.isBlank(alias)) {
            return aliasValidator.isValid(alias, context);
        }
        return uns.getId() != null;
    }
}