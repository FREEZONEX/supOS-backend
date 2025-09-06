package com.supos.uns.service.exportimport.core.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.InstanceField;
import com.supos.common.dto.excel.ExcelUnsWrapDto;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PathUtil;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.parser.data.ValidateFile;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Set;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileReferenceParser
 * @date 2025/4/22 19:19
 */
@Slf4j
public class FileReferenceParser extends AbstractParser {

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

        fileDto.setAlias(StringUtils.isNotBlank(fileDto.getAlias()) ? fileDto.getAlias() : PathUtil.generateAlias(fileDto.getPath(),2));
        CreateTopicDto createTopicDto = fileDto.createTopic();
        createTopicDto.setPathType(Constants.PATH_TYPE_FILE);
        createTopicDto.setDataType(Constants.CITING_TYPE);
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

        Boolean autoDashboard = parseBoolean(fileDto.getAutoDashboard(), false);
        if (autoDashboard == null) {
            context.addError(flagNo, I18nUtils.getMessage("uns.excel.autoDashboard.invalid"));
            return null;
        }
        createTopicDto.setAddDashBoard(autoDashboard);

        createTopicDto.setSave2db(false);

        Boolean mockData = parseBoolean(fileDto.getMockData(), false);
        if (mockData == null) {
            context.addError(flagNo, I18nUtils.getMessage("uns.excel.mockData.invalid"));
            return null;
        }
        createTopicDto.setAddFlow(mockData);

        createTopicDto.setFields(null);

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
        if (ArrayUtils.isEmpty(checkRefer)) {
            context.addError(flagNo, I18nUtils.getMessage("uns.import.not.empty", "refers"));
            return null;
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
        fileDto.setRefers(getValueFromDataMap(dataMap, "refers"));
        fileDto.setDescription(getValueFromDataMap(dataMap, "description"));
        fileDto.setPersistence(getValueFromDataMap(dataMap, "enableHistory"));
        fileDto.setAutoDashboard(getValueFromDataMap(dataMap, "generateDashboard"));
        fileDto.setLabel(getValueFromDataMap(dataMap, "label"));

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
        fileDto.setRefers(getValueFromJsonNode(data, "refers"));
        fileDto.setDescription(getValueFromJsonNode(data, "description"));
        fileDto.setPersistence(getValueFromJsonNode(data, "enableHistory"));
        fileDto.setAutoDashboard(getValueFromJsonNode(data, "generateDashboard"));
        fileDto.setLabel(getValueFromJsonNode(data, "label"));

        ExcelUnsWrapDto wrapDto = check(fileDto, context, parent);
        if (wrapDto != null) {
            context.addUns(wrapDto);
        }
    }
}
