package com.supos.uns.service.exportimport.core.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.supos.common.Constants;
import com.supos.common.dto.FieldDefine;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PathUtil;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.parser.data.ValidateTemplate;
import com.supos.uns.vo.CreateTemplateVo;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TemplateParser
 * @date 2025/4/22 15:27
 */
@Slf4j
public class TemplateParser extends AbstractParser {

    private CreateTemplateVo check(ValidateTemplate templateDto, ExcelImportContext context) {
        String flagNo = templateDto.getFlagNo();
        // 基础校验
        StringBuilder er = null;
        Set<ConstraintViolation<Object>> violations = validator.validate(templateDto);
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

        CreateTemplateVo templateVo = new CreateTemplateVo();
        templateVo.setFlagNo(flagNo);
        templateVo.setName(templateDto.getName());
        templateVo.setAlias(StringUtils.isNotBlank(templateDto.getAlias()) ? templateDto.getAlias() : PathUtil.generateAlias(templateDto.getName(),1));
        templateVo.setDescription(templateDto.getDescription());

        if (templateVo.getAlias().length() > 63) {
            context.addError(flagNo, I18nUtils.getMessage("uns.import.length.limit", "alias", 63));
            return null;
        }
        if (!Constants.ALIAS_PATTERN.matcher(templateVo.getAlias()).matches()) {
            context.addError(flagNo, I18nUtils.getMessage("uns.import.formate.invalid", "alias", I18nUtils.getMessage("uns.import.formate2")));
            return null;
        }

        if (templateVo.getName().length() > 63) {
            context.addError(flagNo, I18nUtils.getMessage("uns.import.length.limit", "name", 63));
            return null;
        }
        if (!Constants.NAME_PATTERN.matcher(templateVo.getName()).matches()) {
            context.addError(flagNo, I18nUtils.getMessage("uns.import.formate.invalid", "name", I18nUtils.getMessage("uns.import.formate1")));
            return null;
        }

        if (context.containTemplateAliasInImportFile(templateVo.getAlias())) {
            // excel 中存在重复的topic
            log.warn(I18nUtils.getMessage("uns.excel.duplicate.item", String.format("%s|%s", ExcelTypeEnum.Template.getCode(), templateDto.getName())));
            return null;
        }

        Triple<Boolean, Integer, FieldDefine[]> checkFieldResult = checkFields(true, flagNo, templateDto.getFields(), context);
        if (checkFieldResult.getLeft()) {
            if (checkFieldResult.getRight() != null) {
                templateVo.setFields(checkFieldResult.getRight());
            } else {
                context.addError(flagNo, I18nUtils.getMessage("uns.field.empty"));
                return null;
            }
        } else {
            return null;
        }

        return templateVo;
    }

    @Override
    public void parseExcel(String flagNo, Map<String, Object> dataMap, ExcelImportContext context) {
        Map<String, CreateTemplateVo> templateMap = new HashMap<>();

        if (isEmptyRow(dataMap)) {
            return;
        }

        ValidateTemplate templateDto = new ValidateTemplate();
        templateDto.setFlagNo(flagNo);
        templateDto.setName(getValueFromDataMap(dataMap, "name"));
        templateDto.setAlias(getValueFromDataMap(dataMap, "alias"));
        templateDto.setFields(getValueFromDataMap(dataMap, "fields"));
        templateDto.setDescription(getValueFromDataMap(dataMap, "description"));

        CreateTemplateVo templateVo = check(templateDto, context);
        if (templateVo != null) {
            context.addTemplateVo(templateVo);
        }
    }

    @Override
    public void parseComplexJson(String flagNo, JsonNode data, ExcelImportContext context, Object parent) {
        Map<String, CreateTemplateVo> templateMap = new HashMap<>();

        if (data == null) {
            return;
        }

        ValidateTemplate templateDto = new ValidateTemplate();
        templateDto.setFlagNo(flagNo);
        templateDto.setName(getValueFromJsonNode(data, "name"));
        templateDto.setAlias(getValueFromJsonNode(data, "alias"));
        templateDto.setFields(getValueFromJsonNode(data, "fields"));
        templateDto.setDescription(getValueFromJsonNode(data, "description"));

        CreateTemplateVo templateVo = check(templateDto, context);
        if (templateVo != null) {
            context.addTemplateVo(templateVo);
        }
    }
}
