package com.supos.uns.service.exportimport.core.parser;

import cn.hutool.core.bean.BeanUtil;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.excel.ExcelNameSpaceDto;
import com.supos.common.dto.excel.ExcelUnsWrapDto;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PathUtil;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import com.supos.uns.service.exportimport.core.parser.AbstractParser;
import com.supos.uns.service.exportimport.json.data.FileTimeseries;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Set;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileTimeseriesParser
 * @date 2025/4/22 19:18
 */
@Slf4j
public class FileTimeseriesParser extends AbstractParser {

    @Override
    public void parseExcel(int batch, int index, Map<String, Object> dataMap, ExcelImportContext context) {
        ExcelTypeEnum excelType = ExcelTypeEnum.FILE_TIMESERIES;
        if (isEmptyRow(dataMap)) {
            return;
        }
        ExcelNameSpaceDto excelNameSpaceDto = BeanUtil.copyProperties(dataMap, ExcelNameSpaceDto.class);
        excelNameSpaceDto.setBatch(batch);
        excelNameSpaceDto.setIndex(index);
        String batchIndex = excelNameSpaceDto.gainBatchIndex();
        {
            StringBuilder er = null;
            Set<ConstraintViolation<Object>> violations = validator.validate(excelNameSpaceDto);
            if (!violations.isEmpty()) {
                if (er == null) {
                    er = new StringBuilder(128);
                }
                addValidErrMsg(er, violations);
            }
            if (er != null) {
                context.addError(batchIndex, er.toString());
                return;
            }
        }
        excelNameSpaceDto.setAlias(StringUtils.isNotBlank(excelNameSpaceDto.getAlias()) ? excelNameSpaceDto.getAlias() : PathUtil.generateAlias(excelNameSpaceDto.getPath(),2));
        CreateTopicDto createTopicDto = excelNameSpaceDto.createTopic();
        ExcelUnsWrapDto wrapDto = new ExcelUnsWrapDto(batchIndex, createTopicDto);

        if (context.getPathInExcel().contains(excelNameSpaceDto.getPath())) {
            // excel 中存在重复的topic
            log.warn(I18nUtils.getMessage("uns.excel.duplicate.item", String.format("%s|%s", excelType.getCode(), excelNameSpaceDto.getPath())));
            return;
        }

        if (StringUtils.isNotBlank(excelNameSpaceDto.getAlias()) && !context.addAlias(excelNameSpaceDto.getAlias())) {
            context.addError(batchIndex, I18nUtils.getMessage("uns.alias.has.exist"));
            return;
        }

        Boolean autoDashboard = getBoolean(dataMap, "autoDashboard", false);
        if (autoDashboard == null) {
            context.addError(batchIndex, I18nUtils.getMessage("uns.excel.autoDashboard.invalid"));
            return;
        }
        createTopicDto.setAddDashBoard(autoDashboard);

        Boolean persistence = getBoolean(dataMap, "persistence", false);
        if (persistence == null) {
            context.addError(batchIndex, I18nUtils.getMessage("uns.excel.persistence.invalid"));
            return;
        }
        createTopicDto.setSave2db(persistence);

        Pair<Boolean, FieldDefine[]> checkFieldResult = checkFields(batchIndex, excelNameSpaceDto.getFields(), context);
        if (checkFieldResult.getLeft()) {
            if (checkFieldResult.getRight() != null) {
                createTopicDto.setFields(checkFieldResult.getRight());
            }
        } else {
            return;
        }

        // 收集模板
        if (StringUtils.isNotBlank(excelNameSpaceDto.getTemplateAlias())) {
            wrapDto.setTemplateAlias(excelNameSpaceDto.getTemplateAlias());
            context.addCheckTemplateAlias(excelNameSpaceDto.getTemplateAlias());
        }

        // 收集标签
        String labelStr = getString(dataMap, "label", "");
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

        createTopicDto.setPathType(2);
        createTopicDto.setDataType(excelType.getDataType());

        context.getUnsList().add(wrapDto);
        context.getUnsMap().put(excelNameSpaceDto.getPath(), wrapDto);
        context.addPath(excelNameSpaceDto.getPath());
        context.addAlias(createTopicDto.getAlias());
    }

    @Override
    public void parseJson(int batch, int index, ExportImportData data, ExcelImportContext context) {
        ExcelTypeEnum excelType = ExcelTypeEnum.FILE_TIMESERIES;
        if (data == null) {
            return;
        }
        FileTimeseries fileTimeseries = (FileTimeseries) data;
        ExcelNameSpaceDto excelNameSpaceDto = BeanUtil.copyProperties(fileTimeseries, ExcelNameSpaceDto.class);
        excelNameSpaceDto.setBatch(batch);
        excelNameSpaceDto.setIndex(index);
        String batchIndex = excelNameSpaceDto.gainBatchIndex();
        {
            StringBuilder er = null;
            Set<ConstraintViolation<Object>> violations = validator.validate(excelNameSpaceDto);
            if (!violations.isEmpty()) {
                if (er == null) {
                    er = new StringBuilder(128);
                }
                addValidErrMsg(er, violations);
            }
            if (er != null) {
                context.addError(batchIndex, er.toString());
                return;
            }
        }
        excelNameSpaceDto.setAlias(StringUtils.isNotBlank(excelNameSpaceDto.getAlias()) ? excelNameSpaceDto.getAlias() : PathUtil.generateAlias(excelNameSpaceDto.getPath(),2));
        CreateTopicDto createTopicDto = excelNameSpaceDto.createTopic();
        ExcelUnsWrapDto wrapDto = new ExcelUnsWrapDto(batchIndex, createTopicDto);

        if (context.getPathInExcel().contains(excelNameSpaceDto.getPath())) {
            // excel 中存在重复的topic
            log.warn(I18nUtils.getMessage("uns.excel.duplicate.item", String.format("%s|%s", excelType.getCode(), excelNameSpaceDto.getPath())));
            return;
        }

        if (StringUtils.isNotBlank(excelNameSpaceDto.getAlias()) && !context.addAlias(excelNameSpaceDto.getAlias())) {
            context.addError(batchIndex, I18nUtils.getMessage("uns.alias.has.exist"));
            return;
        }

        Boolean autoDashboard = parseBoolean(fileTimeseries.getAutoDashboard(), false);
        if (autoDashboard == null) {
            context.addError(batchIndex, I18nUtils.getMessage("uns.excel.autoDashboard.invalid"));
            return;
        }
        createTopicDto.setAddDashBoard(autoDashboard);

        Boolean persistence = parseBoolean(fileTimeseries.getPersistence(), false);
        if (persistence == null) {
            context.addError(batchIndex, I18nUtils.getMessage("uns.excel.persistence.invalid"));
            return;
        }
        createTopicDto.setSave2db(persistence);

        Pair<Boolean, FieldDefine[]> checkFieldResult = checkFields(batchIndex, excelNameSpaceDto.getFields(), context);
        if (checkFieldResult.getLeft()) {
            if (checkFieldResult.getRight() != null) {
                createTopicDto.setFields(checkFieldResult.getRight());
            }
        } else {
            return;
        }

        // 收集模板
        if (StringUtils.isNotBlank(excelNameSpaceDto.getTemplateAlias())) {
            wrapDto.setTemplateAlias(excelNameSpaceDto.getTemplateAlias());
            context.addCheckTemplateAlias(excelNameSpaceDto.getTemplateAlias());
        }

        // 收集标签
        String labelStr = fileTimeseries.getLabel();
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

        createTopicDto.setPathType(2);
        createTopicDto.setDataType(excelType.getDataType());

        context.getUnsList().add(wrapDto);
        context.getUnsMap().put(excelNameSpaceDto.getPath(), wrapDto);
        context.addPath(excelNameSpaceDto.getPath());
        context.addAlias(createTopicDto.getAlias());
    }
}
