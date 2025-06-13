package com.supos.uns.service.exportimport.core.parser;

import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.InstanceField;
import com.supos.common.utils.FieldUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Set;

/**
 * @author sunlifang
 * @version 1.0
 * @description: AbstractParser
 * @date 2025/4/22 15:27
 */
public abstract class AbstractParser implements ParserAble {

    static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    protected boolean isEmptyRow(Map<String, Object> dataMap) {
        if (dataMap == null) {
            return true;
        }
        for (Object value : dataMap.values()) {
            if (value != null) {
                if (StringUtils.isNotBlank(value.toString())) {
                    return false;
                }
            }
        }
        return true;
    }

    protected void addValidErrMsg(StringBuilder er, Set<ConstraintViolation<Object>> violations) {
        for (ConstraintViolation<Object> v : violations) {
            String t = v.getRootBeanClass().getSimpleName();
            String msg = I18nUtils.getMessage(v.getMessage());
            er.append("[{").append(v.getPropertyPath()).append("} ").append(msg).append(']');
        }
    }

    protected Pair<Boolean, FieldDefine[]> checkFields(String batchIndex, String fields, ExcelImportContext context) {
        if (StringUtils.isNotBlank(fields)) {
            FieldDefine[] defineList;
            try {
                defineList = JsonUtil.fromJson(fields, FieldDefine[].class);
            } catch (Exception ex) {
                context.addError(batchIndex, "field json Err:" + ex.getMessage());
                return Pair.of(false, null);
            }
            StringBuilder er = null;
            for (FieldDefine define : defineList) {
                Set<ConstraintViolation<Object>> violations = validator.validate(define);
                if (!violations.isEmpty()) {
                    if (er == null) {
                        er = new StringBuilder(128);
                    }
                    addValidErrMsg(er, violations);
                }
            }
            if (er != null) {
                context.addError(batchIndex, er.toString());
                return Pair.of(false, null);
            }
            String validateMsg = FieldUtils.validateFields(defineList, true);
            if (validateMsg != null) {
                context.addError(batchIndex, validateMsg);
                return Pair.of(false, null);
            }
            return Pair.of(true, defineList);
        }
        return Pair.of(true, null);
    }

    protected Pair<Boolean, InstanceField[]> checkRefers(String batchIndex, String refers, ExcelImportContext context) {
        if (StringUtils.isNotBlank(refers)) {
            InstanceField[] referList;
            try {
                referList = JsonUtil.fromJson(refers, InstanceField[].class);
            } catch (Exception ex) {
                context.addError(batchIndex, "refers json Err:" + ex.getMessage());
                return Pair.of(false, null);
            }

            for (InstanceField refer : referList) {
                if (StringUtils.isBlank(refer.getPath())) {
                    context.addError(batchIndex, I18nUtils.getMessage("uns.refer.path.empty"));
                    return Pair.of(false, null);
                } else {
                    refer.setPath(StringUtils.trim(refer.getPath()));
                }
            }
            return Pair.of(true, referList);
        }
        return Pair.of(true, null);
    }

    protected String getString(Map<String, Object> dataMap, String key, String defaultValue) {
        String finalValue = defaultValue;
        Object value = dataMap.get(key);
        if (value != null) {
            String tempValue = value.toString();
            if (StringUtils.isNotBlank(tempValue)) {
                finalValue = tempValue;
            }
        }

        return finalValue;
    }

    protected Boolean getBoolean(Map<String, Object> dataMap, String key, Boolean defaultValue) {
        Boolean finalValue = defaultValue;
        Object value = dataMap.get(key);
        if (value != null) {
            if (value instanceof Boolean) {
                finalValue = (Boolean) value;
            } else if (value instanceof String) {
                String tempValue = (String) value;
                if (StringUtils.isNotBlank(tempValue)) {
                    if (tempValue.equalsIgnoreCase("true") || tempValue.equalsIgnoreCase("false")) {
                        finalValue = Boolean.valueOf(tempValue);
                    } else {
                        // 无效值
                        finalValue = null;
                    }
                }
            }
        }

        return finalValue;
    }

    protected Boolean parseBoolean(String value, Boolean defaultValue) {
        Boolean finalValue = defaultValue;
        String tempValue = value;
        if (StringUtils.isNotBlank(tempValue)) {
            if (tempValue.equalsIgnoreCase("true") || tempValue.equalsIgnoreCase("false")) {
                finalValue = Boolean.valueOf(tempValue);
            } else {
                // 无效值
                finalValue = null;
            }
        }

        return finalValue;
    }
}
