package com.supos.uns.service.exportimport.core.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.Sets;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.InstanceField;
import com.supos.common.enums.FieldType;
import com.supos.common.utils.FieldUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author sunlifang
 * @version 1.0
 * @description: AbstractParser
 * @date 2025/4/22 15:27
 */
@Slf4j
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

    protected Triple<Boolean, Integer, FieldDefine[]> checkFields(boolean checkBlob, String batchIndex, String fields, ExcelImportContext context) {
        Integer flag = 0;
        if (StringUtils.isNotBlank(fields)) {
            FieldDefine[] defineList;
            try {
                defineList = JsonUtil.fromJson(fields, FieldDefine[].class);

                JsonNode jsonNode = new JsonMapper().readTree(fields.getBytes(StandardCharsets.UTF_8));
                if (jsonNode.isArray()) {
                    Set<String> keys = new HashSet<>();
                    Iterator<JsonNode> fieldNodeIt = jsonNode.iterator();
                    while (fieldNodeIt.hasNext()) {
                        JsonNode fieldNode = fieldNodeIt.next();
                        keys.addAll(Sets.newHashSet(fieldNode.fieldNames()));
                    }
                    flag = FieldUtils.generateFlag(keys.toArray(new String[keys.size()]));
                }
            } catch (Exception ex) {
                log.error("field json Err", ex);
                context.addError(batchIndex, I18nUtils.getMessage("uns.import.formate.invalid1", "fields"));
                return Triple.of(false, null, null);
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
                if (checkBlob) {
                    if (define.getType() == FieldType.BLOB || define.getType() == FieldType.LBLOB) {
                        context.addError(batchIndex, I18nUtils.getMessage("uns.import.field.blob"));
                        return Triple.of(false, null, null);
                    }
                }
            }
            if (er != null) {
                context.addError(batchIndex, er.toString());
                return Triple.of(false, flag, null);
            }
            String validateMsg = FieldUtils.validateFields(defineList, true);
            if (validateMsg != null) {
                context.addError(batchIndex, validateMsg);
                return Triple.of(false, flag, null);
            }
            return Triple.of(true, flag, defineList);
        }
        return Triple.of(true, flag, null);
    }

    protected Pair<Boolean, InstanceField[]> checkRefers(String batchIndex, String refers, ExcelImportContext context) {
        if (StringUtils.isNotBlank(refers)) {
            InstanceField[] referList;
            try {
                referList = JsonUtil.fromJson(refers, InstanceField[].class);
            } catch (Exception ex) {
                log.error("refers json Err", ex);
                context.addError(batchIndex, I18nUtils.getMessage("uns.import.formate.invalid1", "refers"));
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

    protected String getValueFromJsonNode(JsonNode jsonNode, String field) {
        JsonNode valueNode = jsonNode.get(field);
        if (valueNode != null && valueNode.isTextual()) {
            return valueNode.textValue();
        }
        return null;
    }

    protected String getValueFromJsonNodeArray(JsonNode jsonNode, String field) {
        JsonNode valueNode = jsonNode.get(field);
        if (valueNode != null && valueNode.isArray()) {
            return valueNode.toString();
        }
        return null;
    }

    protected String getValueFromDataMap(Map<String, Object> dataMap, String field) {
        Object value = dataMap.get(field);
        if (value != null) {
            return value.toString();
        }
        return null;
    }
}
