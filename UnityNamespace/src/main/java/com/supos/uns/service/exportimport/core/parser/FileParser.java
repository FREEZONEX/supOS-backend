package com.supos.uns.service.exportimport.core.parser;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.system.SystemUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.InstanceField;
import com.supos.common.dto.excel.ExcelUnsWrapDto;
import com.supos.common.utils.FieldUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PathUtil;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.parser.data.ValidateFile;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Map;
import java.util.Set;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileTimeseriesParser
 * @date 2025/4/22 19:18
 */
@Slf4j
public class FileParser extends AbstractParser {

    private static Set<Integer> FILE_TYPE = Set.of(Constants.TIME_SEQUENCE_TYPE, Constants.RELATION_TYPE, Constants.CALCULATION_REAL_TYPE,
            Constants.MERGE_TYPE, Constants.CITING_TYPE, Constants.JSONB_TYPE);

    private Integer checkDataType(ValidateFile fileDto, ExcelImportContext context) {
        String dataTypeStr = fileDto.getDataType();
        if (StringUtils.isBlank(dataTypeStr)) {
            context.addError(fileDto.getFlagNo(), I18nUtils.getMessage("uns.import.dataType.error"));
            return null;
        }
        Integer dataType = null;
        if (NumberUtils.isDigits(dataTypeStr)) {
            try {
                dataType = Integer.valueOf(dataTypeStr);
            } catch (Throwable e) {
                dataType = null;
            }
        }

        if (dataType == null || !FILE_TYPE.contains(dataType)) {
            context.addError(fileDto.getFlagNo(), I18nUtils.getMessage("uns.import.dataType.error"));
            return null;
        }
        return dataType;
    }

    private ExcelUnsWrapDto check(ValidateFile fileDto, ExcelImportContext context, Object parent) {
        String flagNo = fileDto.getFlagNo();

        // 基础校验
        StringBuilder er = null;
        Set<ConstraintViolation<Object>> violations = validator.validate(fileDto);
        if (!violations.isEmpty()) {
            if (er == null) {
                er = new StringBuilder(128);
            }
            addValidErrMsg(er, violations);
        }
        if (er != null) {
            context.addError(flagNo, er.toString());
            return null;
        }

        Integer dataType = checkDataType(fileDto, context);
        if (dataType == null) {
            return null;
        }

        fileDto.setAlias(StringUtils.isNotBlank(fileDto.getAlias()) ? fileDto.getAlias() : PathUtil.generateAlias(fileDto.getPath(),2));
        CreateTopicDto createTopicDto = fileDto.createTopic();
        createTopicDto.setPathType(Constants.PATH_TYPE_FILE);
        createTopicDto.setDataType(dataType);
        createTopicDto.setParentDataType(fileDto.getParentDataType());
        ExcelUnsWrapDto wrapDto = new ExcelUnsWrapDto(createTopicDto);

        // 校验path是否重复
        if (context.containPathInImportFile(fileDto.getPath())) {
            // excel 中存在重复的topic
            context.addError(flagNo, I18nUtils.getMessage("uns.import.exist", "namespace", fileDto.getPath()));
            return null;
        }

        // 校验别名是否重复
        if (context.containAliasInImportFile(fileDto.getAlias())) {
            context.addError(flagNo, I18nUtils.getMessage("uns.alias.has.exist"));
            return null;
        }

        if (parent != null) {
            ExcelUnsWrapDto parentWrap = (ExcelUnsWrapDto) parent;
            if (!fileDto.getPath().startsWith(parentWrap.getPath())) {
                context.addError(flagNo, I18nUtils.getMessage("uns.import.formate.invalid1", "namespace"));
                return null;
            }
            createTopicDto.setParentAlias(parentWrap.getAlias());
        }

        if (SystemUtil.getBoolean("SYS_OS_ENABLE_AUTO_CATEGORIZATION", false)) {
            if (fileDto.getParentDataType() == null) {
                context.addError(flagNo, I18nUtils.getMessage("uns.excel.parentDataType.is.blank"));
                return null;
            } else {
                createTopicDto.setParentDataType(fileDto.getParentDataType());
            }
        }

        if (dataType != Constants.MERGE_TYPE && dataType != Constants.CITING_TYPE) {
            Triple<Boolean, Integer, FieldDefine[]> checkFieldResult = checkFields(dataType == Constants.TIME_SEQUENCE_TYPE ? false : true,flagNo, fileDto.getFields(), context);
            if (checkFieldResult.getLeft()) {
                if (dataType == Constants.CALCULATION_REAL_TYPE) {
                    if (ArrayUtils.isEmpty(checkFieldResult.getRight()) || checkFieldResult.getRight().length > 1) {
                        context.addError(flagNo, I18nUtils.getMessage("uns.import.field.calculation.invalid"));
                        return null;
                    }
                }
                if (checkFieldResult.getRight() != null) {
                    createTopicDto.setExtendFieldUsed(FieldUtils.parseFlag(checkFieldResult.getMiddle()));
                    createTopicDto.setFields(checkFieldResult.getRight());
                }
            } else {
                return null;
            }
        } else {
            // 聚合和引用不需要属性
            createTopicDto.setFields(null);
        }

        // 校验订阅属性
/*        Boolean subscribe = parseBoolean(fileDto.getSubscribe(), false);
        if (subscribe == null) {
            context.addError(flagNo, I18nUtils.getMessage("uns.excel.persistence.invalid"));
            return null;
        }
        createTopicDto.setSubscribeEnable(subscribe);*/

        if (dataType == Constants.MERGE_TYPE || dataType == Constants.CITING_TYPE) {
            createTopicDto.setSave2db(false);
        } else {
            Boolean persistence = parseBoolean(fileDto.getPersistence(), false);
            if (persistence == null) {
                context.addError(flagNo, I18nUtils.getMessage("uns.excel.persistence.invalid"));
                return null;
            }
            createTopicDto.setSave2db(persistence);
        }


        // 表达式
        String expression = fileDto.getExpression();
        if (StringUtils.isNotBlank(expression)) {
            createTopicDto.setExpression(expression);
        }

        // 收集模板
        if (StringUtils.isNotBlank(fileDto.getTemplateAlias())) {
            wrapDto.setTemplateAlias(fileDto.getTemplateAlias());
            context.addCheckTemplateAlias(fileDto.getTemplateAlias());
        }

        // 收集标签
        String labelStr = fileDto.getLabel();
        if (StringUtils.isNotBlank(labelStr)) {
            String[] labels = StringUtils.split(labelStr, ',');
            if (labels.length > 0) {
                for (String label : labels) {
                    if (StringUtils.isNotBlank(label)) {
                        context.addCheckLabel(label);
                        wrapDto.addLabel(label);
                    }
                }
            }
        }

        // refers收集
        String refersStr = fileDto.getRefers();
        InstanceField[] checkRefer = null;
        if (StringUtils.isNotBlank(refersStr)) {
            Pair<Boolean, InstanceField[]> checkReferResult = checkRefers(flagNo, refersStr, context);
            if (checkReferResult.getLeft()) {
                if (checkReferResult.getRight() != null) {
                    checkRefer = checkReferResult.getRight();
                    wrapDto.setRefers(checkReferResult.getRight());
                    for (InstanceField instanceField : checkReferResult.getRight()) {
                        if (StringUtils.isNotBlank(instanceField.getAlias())) {
                            context.addCheckReferAlias(instanceField.getAlias());
                        }
                        if (StringUtils.isNotBlank(instanceField.getPath())) {
                            context.addCheckReferPath(instanceField.getPath());
                        }
                    }
                }
            } else {
                return null;
            }
        }

        if (dataType == Constants.MERGE_TYPE || dataType == Constants.CITING_TYPE) {
            if (ArrayUtils.isEmpty(checkRefer)) {
                context.addError(flagNo, I18nUtils.getMessage("uns.import.not.empty", "refers"));
                return null;
            }
        }

        // frequency
        if (dataType == Constants.MERGE_TYPE) {
            String frequencyStr = fileDto.getFrequency();
            if (StringUtils.isBlank(frequencyStr)) {
                context.addError(flagNo, I18nUtils.getMessage("uns.field.frequency.empty"));
                return null;
            } else {
                frequencyStr = frequencyStr.trim();
                frequencyStr = frequencyStr.toLowerCase();
                String timePattern = "\\b([1-9]\\d*[msh])\\b";
                if (!frequencyStr.matches(timePattern)) {
                    context.addError(flagNo, I18nUtils.getMessage("uns.field.frequency.invalid"));
                    return null;
                }
                createTopicDto.setFrequency(frequencyStr);
            }
        }

        return wrapDto;
    }

    @Override
    public void parseExcel(String flagNo, Map<String, Object> dataMap, ExcelImportContext context) {
        if (isEmptyRow(dataMap)) {
            return;
        }
        ValidateFile fileDto = new ValidateFile();
        fileDto.setFlagNo(flagNo);
        fileDto.setPath(getValueFromDataMap(dataMap, "namespace"));
        fileDto.setAlias(getValueFromDataMap(dataMap, "alias"));
        fileDto.setDisplayName(getValueFromDataMap(dataMap, "displayName"));
        fileDto.setTemplateAlias(getValueFromDataMap(dataMap, "templateAlias"));
        fileDto.setDescription(getValueFromDataMap(dataMap, "description"));
        fileDto.setFields(getValueFromDataMap(dataMap, "fields"));
        fileDto.setDataType(getValueFromDataMap(dataMap, "dataType"));
        fileDto.setRefers(getValueFromDataMap(dataMap, "refers"));
        fileDto.setExpression(getValueFromDataMap(dataMap, "expression"));
        fileDto.setLabel(getValueFromDataMap(dataMap, "label"));
        fileDto.setFrequency(getValueFromDataMap(dataMap, "frequency"));
        fileDto.setPersistence(getValueFromDataMap(dataMap, "enableHistory"));
        fileDto.setAutoDashboard(getValueFromDataMap(dataMap, "generateDashboard"));
        fileDto.setMockData(getValueFromDataMap(dataMap, "mockData"));
        Object parentDataType = dataMap.get("parentDataType");
        Integer pDataType = ObjectUtil.isEmpty(parentDataType) ? null : Integer.parseInt(parentDataType.toString());
        fileDto.setParentDataType(pDataType);

        ExcelUnsWrapDto wrapDto = check(fileDto, context, null);
        if (wrapDto != null) {
            context.addUns(wrapDto);
        }
    }

    @Override
    public void parseComplexJson(String flagNo, JsonNode data, ExcelImportContext context, Object parent) {
        if (data == null) {
            return;
        }

        ((ObjectNode) data).remove("error");

        ValidateFile fileDto = new ValidateFile();
        fileDto.setFlagNo(flagNo);
        fileDto.setPath(getValueFromJsonNode(data, "namespace"));
        fileDto.setAlias(getValueFromJsonNode(data, "alias"));
        fileDto.setDisplayName(getValueFromJsonNode(data, "displayName"));
        fileDto.setTemplateAlias(getValueFromJsonNode(data, "templateAlias"));
        fileDto.setDescription(getValueFromJsonNode(data, "description"));
        fileDto.setFields(getValueFromJsonNode(data, "fields"));
        fileDto.setDataType(getValueFromJsonNode(data, "dataType"));
        fileDto.setRefers(getValueFromJsonNode(data, "refers"));
        fileDto.setExpression(getValueFromJsonNode(data, "expression"));
        fileDto.setLabel(getValueFromJsonNode(data, "label"));
        fileDto.setFrequency(getValueFromJsonNode(data, "frequency"));
        fileDto.setPersistence(getValueFromJsonNode(data, "enableHistory"));
        fileDto.setAutoDashboard(getValueFromJsonNode(data, "generateDashboard"));
        fileDto.setMockData(getValueFromJsonNode(data, "mockData"));
        String parentDataType = getValueFromJsonNode(data, "parentDataType");
        Integer pDataType = ObjectUtil.isEmpty(parentDataType) ? null : Integer.parseInt(parentDataType);
        fileDto.setParentDataType(pDataType);
        ExcelUnsWrapDto wrapDto = check(fileDto, context, parent);
        if (wrapDto != null) {
            context.addUns(wrapDto);
        }
    }
}
