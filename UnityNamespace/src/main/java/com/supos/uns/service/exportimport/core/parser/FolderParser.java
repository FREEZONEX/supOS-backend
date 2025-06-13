package com.supos.uns.service.exportimport.core.parser;

import cn.hutool.core.bean.BeanUtil;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.excel.ExcelFolderDto;
import com.supos.common.dto.excel.ExcelUnsWrapDto;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PathUtil;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import com.supos.uns.service.exportimport.core.parser.AbstractParser;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Set;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FolderParser
 * @date 2025/4/22 15:27
 */
@Slf4j
public class FolderParser extends AbstractParser {

    @Override
    public void parseExcel(int batch, int index, Map<String, Object> dataMap, ExcelImportContext context) {
        ExcelTypeEnum excelType = ExcelTypeEnum.Folder;
        if (isEmptyRow(dataMap)) {
            return;
        }

        ExcelFolderDto excelFolderDto = BeanUtil.copyProperties(dataMap, ExcelFolderDto.class);
        excelFolderDto.setBatch(batch);
        excelFolderDto.setIndex(index);
        excelFolderDto.trim();
        String batchIndex = excelFolderDto.gainBatchIndex();

        {
            StringBuilder er = null;
            Set<ConstraintViolation<Object>> violations = validator.validate(excelFolderDto);
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
        excelFolderDto.setAlias(StringUtils.isNotBlank(excelFolderDto.getAlias()) ? excelFolderDto.getAlias() : PathUtil.generateAlias(excelFolderDto.getPath(),0));
        CreateTopicDto createTopicDto = excelFolderDto.createTopic();
        if (StringUtils.length(createTopicDto.getName()) > 63) {
            context.addError(batchIndex, I18nUtils.getMessage("uns.folder.length.limit.exceed", 63));
            return;
        }
        ExcelUnsWrapDto wrapDto = new ExcelUnsWrapDto(batchIndex, createTopicDto);

        if (context.getPathInExcel().contains(excelFolderDto.getPath())) {
            // excel 中存在重复的topic
            log.warn(I18nUtils.getMessage("uns.excel.duplicate.item", String.format("%s|%s", excelType.getCode(), excelFolderDto.getPath())));
            return;
        }

        if (StringUtils.isNotBlank(excelFolderDto.getAlias()) && !context.addAlias(excelFolderDto.getAlias())) {
            context.addError(batchIndex, I18nUtils.getMessage("uns.alias.has.exist"));
            return;
        }

        Pair<Boolean, FieldDefine[]> checkFieldResult = checkFields(batchIndex, excelFolderDto.getFields(), context);
        if (checkFieldResult.getLeft()) {
            if (checkFieldResult.getRight() != null) {
                createTopicDto.setFields(checkFieldResult.getRight());
            } else {
                createTopicDto.setFields(new FieldDefine[]{});
            }
        } else {
            return;
        }

        // 收集模板
        if (StringUtils.isNotBlank(excelFolderDto.getTemplateAlias())) {
            wrapDto.setTemplateAlias(excelFolderDto.getTemplateAlias());
            context.addCheckTemplateAlias(excelFolderDto.getTemplateAlias());
        }


        createTopicDto.setPathType(0);
        createTopicDto.setDataType(excelType.getDataType());

        context.getUnsList().add(wrapDto);
        context.getUnsMap().put(excelFolderDto.getPath(), wrapDto);
        context.addPath(excelFolderDto.getPath());
        context.addAlias(createTopicDto.getAlias());
    }

    @Override
    public void parseJson(int batch, int index, ExportImportData data, ExcelImportContext context) {
        ExcelTypeEnum excelType = ExcelTypeEnum.Folder;
        if (data == null) {
            return;
        }

        ExcelFolderDto excelFolderDto = BeanUtil.copyProperties(data, ExcelFolderDto.class);
        excelFolderDto.setBatch(batch);
        excelFolderDto.setIndex(index);
        excelFolderDto.trim();
        String batchIndex = excelFolderDto.gainBatchIndex();

        {
            StringBuilder er = null;
            Set<ConstraintViolation<Object>> violations = validator.validate(excelFolderDto);
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
        excelFolderDto.setAlias(StringUtils.isNotBlank(excelFolderDto.getAlias()) ? excelFolderDto.getAlias() : PathUtil.generateAlias(excelFolderDto.getPath(),0));
        CreateTopicDto createTopicDto = excelFolderDto.createTopic();
        ExcelUnsWrapDto wrapDto = new ExcelUnsWrapDto(batchIndex, createTopicDto);

        if (context.getPathInExcel().contains(excelFolderDto.getPath())) {
            // excel 中存在重复的topic
            log.warn(I18nUtils.getMessage("uns.excel.duplicate.item", String.format("%s|%s", excelType.getCode(), excelFolderDto.getPath())));
            return;
        }

        if (StringUtils.isNotBlank(excelFolderDto.getAlias()) && !context.addAlias(excelFolderDto.getAlias())) {
            context.addError(batchIndex, I18nUtils.getMessage("uns.alias.has.exist"));
            return;
        }

        Pair<Boolean, FieldDefine[]> checkFieldResult = checkFields(batchIndex, excelFolderDto.getFields(), context);
        if (checkFieldResult.getLeft()) {
            if (checkFieldResult.getRight() != null) {
                createTopicDto.setFields(checkFieldResult.getRight());
            } else {
                createTopicDto.setFields(new FieldDefine[]{});
            }
        } else {
            return;
        }

        // 收集模板
        if (StringUtils.isNotBlank(excelFolderDto.getTemplateAlias())) {
            wrapDto.setTemplateAlias(excelFolderDto.getTemplateAlias());
            context.addCheckTemplateAlias(excelFolderDto.getTemplateAlias());
        }


        createTopicDto.setPathType(0);
        createTopicDto.setDataType(excelType.getDataType());

        context.getUnsList().add(wrapDto);
        context.getUnsMap().put(excelFolderDto.getPath(), wrapDto);
        context.addPath(excelFolderDto.getPath());
        context.addAlias(createTopicDto.getAlias());
    }
}
