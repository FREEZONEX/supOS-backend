package com.supos.common.annotation;

import com.supos.common.Constants;
import com.supos.common.utils.PathUtil;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static com.supos.common.utils.ConstraintErrTipUtils.setErrMsg;

public class ConstraintTopicNameValidator implements ConstraintValidator<TopicNameValidator, String> {

    @Override
    public boolean isValid(String topic, ConstraintValidatorContext context) {
        if (topic == null) {
            return true;
        }

        if (!Constants.useAliasAsTopic) {
            if (topic.length() > 190) {
                setErrMsg(context, "path", "uns.topic.length.limit.exceed");
                return false;
            }
        }
        return Constants.useAliasAsTopic || PathUtil.validTopicFormate(topic, null);
    }
}