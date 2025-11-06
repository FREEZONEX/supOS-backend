package com.supos.uns.service.exportimport.core.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.excel.ExcelUnsWrapDto;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PathUtil;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.ExportImportHelper;
import com.supos.uns.service.exportimport.core.parser.data.ValidateFolder;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FolderParser
 * @date 2025/4/22 15:27
 */
@Slf4j
public class FolderParser extends AbstractParser {

    private FileParser fileParser = new FileParser();

    private ExcelUnsWrapDto check(ValidateFolder folderDto, ExcelImportContext context, Object parent) {
        String flagNo = folderDto.getFlagNo();
        ExcelTypeEnum excelType = ExcelTypeEnum.Folder;
        // 基础校验
        StringBuilder er = null;
        Set<ConstraintViolation<Object>> violations = validator.validate(folderDto);
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
        folderDto.setAlias(StringUtils.isNotBlank(folderDto.getAlias()) ? folderDto.getAlias() : PathUtil.generateAlias(folderDto.getPath(),0));
        CreateTopicDto createTopicDto = folderDto.createTopic();
        if (StringUtils.length(createTopicDto.getName()) > 63) {
            context.addError(flagNo, I18nUtils.getMessage("uns.folder.length.limit.exceed", 63));
            return null;
        }
        if (createTopicDto.getName() != null) {
            if (StringUtils.equals("label", createTopicDto.getName()) || StringUtils.equals("template", createTopicDto.getName())) {
                context.addError(flagNo, I18nUtils.getMessage("uns.folder.reserved.word"));
                return null;
            }
        }


        // 校验path是否重复
        if (context.containPathInImportFile(folderDto.getPath())) {
            // excel 中存在重复的path
            context.addError(flagNo, I18nUtils.getMessage("uns.import.exist", "namespace", folderDto.getPath()));
            return null;
        }

        // 校验别名是否重复
        if (context.containAliasInImportFile(folderDto.getAlias())) {
            context.addError(flagNo, I18nUtils.getMessage("uns.alias.has.exist"));
            return null;
        }

        if (parent != null) {
            ExcelUnsWrapDto parentWrap = (ExcelUnsWrapDto) parent;
            if (!folderDto.getPath().startsWith(parentWrap.getPath())) {
                context.addError(flagNo, I18nUtils.getMessage("uns.import.formate.invalid1", "namespace"));
                return null;
            }
            createTopicDto.setParentAlias(parentWrap.getAlias());
        }

        // 校验属性
        Triple<Boolean, Integer, FieldDefine[]> checkFieldResult = checkFields(true, flagNo, folderDto.getFields(), context);
        if (checkFieldResult.getLeft()) {
            if (checkFieldResult.getRight() != null) {
                createTopicDto.setFields(checkFieldResult.getRight());
            } else {
                createTopicDto.setFields(new FieldDefine[]{});
            }
        } else {
            return null;
        }

        // 扩展属性
        String extendpropertiesStr = folderDto.getExtendproperties();
        LinkedHashMap<String, Object> extend = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(extendpropertiesStr)) {
            if (!JSONObject.isValidObject(extendpropertiesStr)) {
                context.addError(flagNo, I18nUtils.getMessage("uns.create.extend.invalid"));
                return null;
            }
            JSONObject jsonObject = JSON.parseObject(extendpropertiesStr);
            if (jsonObject != null && !jsonObject.isEmpty()) {

                for (Map.Entry<String, Object> e : jsonObject.entrySet()) {
                    if (e instanceof JSONArray) {
                        context.addError(flagNo, I18nUtils.getMessage("uns.create.extend.invalid"));
                        return null;
                    } else {
                        extend.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
        if (extend.size() > 3) {
            context.addError(flagNo, I18nUtils.getMessage("uns.create.extend.num.limit"));
            return null;
        }
        createTopicDto.setExtend(extend);

        ExcelUnsWrapDto wrapDto = new ExcelUnsWrapDto(createTopicDto);
        // 收集模板
        if (StringUtils.isNotBlank(folderDto.getTemplateAlias())) {
            wrapDto.setTemplateAlias(folderDto.getTemplateAlias());
            context.addCheckTemplateAlias(folderDto.getTemplateAlias());
        }

        createTopicDto.setPathType(0);
        if (StringUtils.isNotBlank(folderDto.getDataType())) {
            createTopicDto.setDataType(Integer.parseInt(folderDto.getDataType()));
        }
        return wrapDto;
    }

    @Override
    public void parseExcel(String flagNo, Map<String, Object> dataMap, ExcelImportContext context) {
        if (isEmptyRow(dataMap)) {
            return;
        }

        ValidateFolder folderDto = new ValidateFolder();
        folderDto.setFlagNo(flagNo);
        folderDto.setPath(getValueFromDataMap(dataMap, "namespace"));
        folderDto.setAlias(getValueFromDataMap(dataMap, "alias"));
        folderDto.setDisplayName(getValueFromDataMap(dataMap, "displayName"));
        folderDto.setTemplateAlias(getValueFromDataMap(dataMap, "templateAlias"));
        folderDto.setDescription(getValueFromDataMap(dataMap, "description"));
        folderDto.setFields(getValueFromDataMap(dataMap, "fields"));
        folderDto.setDataType(getValueFromDataMap(dataMap, "dataType"));
        folderDto.trim();



        ExcelUnsWrapDto wrapDto = check(folderDto, context, null);

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

        ValidateFolder folderDto = new ValidateFolder();
        folderDto.setFlagNo(flagNo);
        folderDto.setPath(getValueFromJsonNode(data, "namespace"));
        folderDto.setAlias(getValueFromJsonNode(data, "alias"));
        folderDto.setDisplayName(getValueFromJsonNode(data, "displayName"));
        folderDto.setTemplateAlias(getValueFromJsonNode(data, "templateAlias"));
        folderDto.setDescription(getValueFromJsonNode(data, "description"));
        folderDto.setFields(getValueFromJsonNode(data, "fields"));
        folderDto.trim();

        ExcelUnsWrapDto wrapDto = check(folderDto, context, parent);

        if (wrapDto != null) {
            context.addUns(wrapDto);

            // 处理子节点
            JsonNode childrenNode = data.get("children");
            if (childrenNode != null && childrenNode.isArray()) {
                AtomicInteger index = new AtomicInteger(0);
                Iterator<JsonNode> iterator = childrenNode.iterator();
                while (iterator.hasNext()) {
                    JsonNode childNode = iterator.next();
                    String childFlagNo = String.format("%s-children-%d", flagNo, index.getAndIncrement());
                    JsonNode typeNode = childNode.get("type");
                    if (typeNode == null || !typeNode.isTextual()) {
                        context.addError(childFlagNo, I18nUtils.getMessage("uns.import.type.error"));
                        return;
                    }

                    String type = typeNode.textValue();
                    if (!ExportImportHelper.TYPE_FOLDER.equals(type) && !ExportImportHelper.TYPE_FILE.equals(type)) {
                        context.addError(childFlagNo, I18nUtils.getMessage("uns.import.type.error"));
                        return;
                    }

                    switch (type) {
                        case ExportImportHelper.TYPE_FOLDER:
                            new FolderParser().parseComplexJson(childFlagNo, childNode, context, wrapDto);
                            break;
                        case ExportImportHelper.TYPE_FILE:
                            fileParser.parseComplexJson(childFlagNo, childNode, context, wrapDto);
                            break;
                    }
                }
            }
        }
    }
}
