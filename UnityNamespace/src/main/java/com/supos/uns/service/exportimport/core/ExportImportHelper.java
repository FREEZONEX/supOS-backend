package com.supos.uns.service.exportimport.core;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.Constants;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.InstanceField;
import com.supos.common.enums.DataTypeEnum;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.enums.FolderDataType;
import com.supos.common.utils.FieldUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.exportimport.core.entity.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelUtil
 * @date 2025/1/10 13:54
*/

public class ExportImportHelper {

    public static final String TYPE_FOLDER = "folder";
    public static final String TYPE_FILE = "file";

    private static List<String> TEMPLATE_INDEX = new LinkedList<>();
    private static List<String> LABEL_INDEX = new LinkedList<>();
    private static List<String> FOLDER_INDEX = new LinkedList<>();
    private static List<String> FILE_INDEX = new LinkedList<>();
    private static List<String> FILE_TIMESERIES_INDEX = new LinkedList<>();
    private static List<String> FILE_RELATION_INDEX = new LinkedList<>();
    private static List<String> FILE_CALCULATE_INDEX = new LinkedList<>();
    private static List<String> FILE_AGGREGATION_INDEX = new LinkedList<>();
    private static List<String> FILE_REFERENCE_INDEX = new LinkedList<>();
    private static List<String> FILE_JSONB_INDEX = new LinkedList<>();

    public static List<String> EXPLANATION = new LinkedList<>();

    static {
        // 模板列
        TEMPLATE_INDEX.addAll(getFields(ExcelTypeEnum.Template));

        // 标签列
        LABEL_INDEX.addAll(getFields(ExcelTypeEnum.Label));

        // 文件夹列
        FOLDER_INDEX.addAll(getFields(ExcelTypeEnum.Folder));

        FILE_INDEX.addAll(getFields(ExcelTypeEnum.File));
        FILE_TIMESERIES_INDEX.addAll(getFields(ExcelTypeEnum.FILE_TIMESERIES));
        FILE_RELATION_INDEX.addAll(getFields(ExcelTypeEnum.FILE_RELATION));
        FILE_CALCULATE_INDEX.addAll(getFields(ExcelTypeEnum.FILE_CALCULATE));
        FILE_AGGREGATION_INDEX.addAll(getFields(ExcelTypeEnum.FILE_AGGREGATION));
        FILE_REFERENCE_INDEX.addAll(getFields(ExcelTypeEnum.FILE_REFERENCE));
        FILE_JSONB_INDEX.addAll(getFields(ExcelTypeEnum.FILE_JSONB));


        EXPLANATION.add("uns.excel.explanation.template.alias");
        EXPLANATION.add("uns.excel.explanation.folder.path");
        EXPLANATION.add("uns.excel.explanation.folder.alias");
        EXPLANATION.add("uns.excel.explanation.folder.templatealias");
        EXPLANATION.add("uns.excel.explanation.file.alias");
        EXPLANATION.add("uns.excel.explanation.file.templatealias");
        EXPLANATION.add("uns.excel.explanation.file.refers");
        EXPLANATION.add("uns.excel.explanation.file.expression");
        EXPLANATION.add("uns.excel.explanation.file.frequency");
        EXPLANATION.add("uns.excel.explanation.file.label");

    }

    public static int errorIndex(ExcelTypeEnum excelTypeEnum) {
        List<String> fields = getFields(excelTypeEnum);
        return fields.size();
    }

    private static List<String> getFields(ExcelTypeEnum excelTypeEnum) {
        Class<? extends ExportImportData> clazz = null;
        switch (excelTypeEnum) {
            case Template:
                clazz = TemplateData.class;
                break;
            case Label:
                clazz = LabelData.class;
                break;
            case Folder:
                clazz = FolderData.class;
                break;
            case File:
                clazz = FileData.class;
                break;
            case FILE_TIMESERIES:
                clazz = FileTimeseriesData.class;
                break;
            case FILE_RELATION:
                clazz = FileRelationData.class;
                break;
            case FILE_CALCULATE:
                clazz = FileCalculateData.class;
                break;
            case FILE_AGGREGATION:
                clazz = FileAggregationData.class;
                break;
            case FILE_REFERENCE:
                clazz = FileReferenceData.class;
                break;
            case FILE_JSONB:
                clazz = FileJsonbData.class;
                break;
        }

        return Arrays.stream(clazz.getDeclaredFields()).filter(field -> {
            return field.getAnnotation(ExcelProperty.class) != null;
        }).map(Field::getName).collect(Collectors.toList());
    }

    /**
     * 校验表头是否正确
     *
     * @param excelType
     * @param heads
     * @return
     */

    public static boolean checkHead(ExcelTypeEnum excelType, List<Object> heads) {
        List<String> needHeads = new ArrayList<>();
        List<String> tempHeads = heads != null ? heads.stream().map(Object::toString).collect(Collectors.toList()) : new ArrayList<>();
        switch (excelType) {
            case Template:
                needHeads = TEMPLATE_INDEX;
                break;
            case Label:
                needHeads = LABEL_INDEX;
                break;
            case Folder:
                needHeads = FOLDER_INDEX;
                break;
            case File:
                needHeads = FILE_INDEX;
                break;
            case FILE_TIMESERIES:
                needHeads = FILE_TIMESERIES_INDEX;
                break;
            case FILE_RELATION:
                needHeads = FILE_RELATION_INDEX;
                break;
            case FILE_CALCULATE:
                needHeads = FILE_CALCULATE_INDEX;
                break;
            case FILE_AGGREGATION:
                needHeads = FILE_AGGREGATION_INDEX;
                break;
            case FILE_REFERENCE:
                needHeads = FILE_REFERENCE_INDEX;
                break;
            case FILE_JSONB:
                needHeads = FILE_JSONB_INDEX;
                break;
        }

        for (String needHead : needHeads) {
            if (!tempHeads.contains(needHead)) {
                return false;
            }
        }
        return true;
    }

    /*
     * 封装导出数据
     * @param unsPo
     * @param context
     * @return
     */
    public static ExportImportData wrapData(ExcelTypeEnum excelTypeEnum, UnsPo unsPo, ExcelExportContext context) {
        ExportImportData exportDataWrapper = null;
        switch (excelTypeEnum) {
            case Template:
                exportDataWrapper = wrapTemplateData(unsPo);
                break;
            case Folder:
                exportDataWrapper = wrapFolderData(unsPo, context);
                break;
            case File:
                exportDataWrapper = wrapFileData(unsPo, context);
                break;
            case FILE_TIMESERIES:
                exportDataWrapper = wrapFileTimeseriesData(unsPo, context);
                break;
            case FILE_RELATION:
                exportDataWrapper = wrapFileRelationData(unsPo, context);
                break;
            case FILE_CALCULATE:
                exportDataWrapper = wrapFileCalculateData(unsPo, context);
                break;
            case FILE_AGGREGATION:
                exportDataWrapper = wrapFileAggregationData(unsPo, context);
                break;
            case FILE_REFERENCE:
                exportDataWrapper = wrapFileReferenceData(unsPo, context);
                break;
            case FILE_JSONB:
                exportDataWrapper = wrapFileJsonbData(unsPo, context);
                break;
        }
        return exportDataWrapper;
    }

    private static String replaceBlank(String value, String defaultValue) {
        return StringUtils.isNotBlank(value) ? value : defaultValue;
    }

    private static String gainFlagValue(Integer withFlags, String flag) {
        String value = "";
        if (withFlags != null) {
            if (flag.equals("persistence")) {
                value = Constants.withSave2db(withFlags) ? "TRUE" : "FALSE";
            } else if (flag.equals("dashboard")) {
                value = Constants.withDashBoard(withFlags) ? "TRUE" : "FALSE";
            } else if (flag.equals("mockData")) {
                value = Constants.withFlow(withFlags) ? "TRUE" : "FALSE";
            }
        }
        return value;
    }

    /**
     * 模板
     * @param unsPo
     * @return
     */
    private static ExportImportData wrapTemplateData(UnsPo unsPo) {
        TemplateData data = new TemplateData();
        data.setAlias(replaceBlank(unsPo.getAlias(), ""));
        data.setName(replaceBlank(unsPo.getName(), ""));
        data.setFields(FieldUtils.filterSystemField(unsPo.getFields()));
        data.setDescription(replaceBlank(unsPo.getDescription(), ""));
        return data;
    }


    /**
     *
     * @param label
     * @return
     */
    public static ExportImportData wrapLabelData(UnsLabelPo label) {
        LabelData data = new LabelData();
        data.setName(replaceBlank(label.getLabelName(), ""));

        return data;
    }

    /*
     * 文件夹
     *
     * @param unsPo
     * @return
     */

    private static ExportImportData wrapFolderData(UnsPo unsPo, ExcelExportContext context) {
        Map<Long, UnsPo> templateMap = context.getTemplateMap();

        FolderData data = new FolderData();

        data.setAlias(replaceBlank(unsPo.getAlias(), ""));
        data.setDisplayName(replaceBlank(unsPo.getDisplayName(), ""));
        if (unsPo.getModelId() != null) {
            UnsPo template = templateMap.get(unsPo.getModelId());
            data.setTemplateAlias(template != null ? template.getAlias() : "");
        } else {
            data.setTemplateAlias("");
        }
        //data.setNamespace(replaceBlank(unsPo.getPath(), ""));
        data.setName(replaceBlank(unsPo.getName(), ""));
        data.setFields(FieldUtils.filterSystemField(unsPo.getFields()));
        data.setDescription(replaceBlank(unsPo.getDescription(), ""));
        data.setDataType(FolderDataType.getFolderDataType(unsPo.getDataType()).name());
        data.setType(TYPE_FOLDER);
        return data;
    }

    /**
     * 文件
     *
     * @param unsPo
     * @return
    */
    private static ExportImportData wrapFileData(UnsPo unsPo, ExcelExportContext context) {
        Map<Long, UnsPo> templateMap = context.getTemplateMap();
        Map<Long, Set<String>> unsLabelNamesMap = context.getUnsLabelNamesMap();

        FileData data = new FileData();

        data.setAlias(replaceBlank(unsPo.getAlias(), ""));
        data.setDisplayName(replaceBlank(unsPo.getDisplayName(), ""));
        if (unsPo.getModelId() != null) {
            UnsPo template = templateMap.get(unsPo.getModelId());
            data.setTemplateAlias(template != null ? template.getAlias() : "");
        } else {
            data.setTemplateAlias("");
        }
        //data.setNamespace(replaceBlank(unsPo.getPath(), ""));
        data.setName(replaceBlank(unsPo.getName(), ""));
        data.setDataType(String.valueOf(DataTypeEnum.parse(unsPo.getDataType()).name()));

        data.setEnableHistory(gainFlagValue(unsPo.getWithFlags(), "persistence"));
        data.setGenerateDashboard(gainFlagValue(unsPo.getWithFlags(), "dashboard"));
        data.setMockData(gainFlagValue(unsPo.getWithFlags(), "mockData"));

        if (StringUtils.isNotBlank(unsPo.getExpression())) {
            data.setExpression(unsPo.getExpression());
        } else {
            data.setExpression("");
        }

        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            data.setLabel(StringUtils.join(labels, ','));
        } else {
            data.setLabel("");
        }

        if (unsPo.getDataType() == Constants.MERGE_TYPE) {
            String frequencyStr = "";
            String protocol = unsPo.getProtocol();
            if (JSONObject.isValidObject(protocol)) {
                JSONObject jsonObject = JSONObject.parseObject(protocol);
                Object frequency =jsonObject.get("frequency");
                if (frequency != null) {
                    frequencyStr = frequency.toString();
                }
            }
            data.setFrequency(frequencyStr);
        } else {
            data.setFrequency("");
        }
        if (unsPo.getDataType() != Constants.JSONB_TYPE) {
            data.setFields(FieldUtils.filterSystemField(unsPo.getFields()));
        }
        data.setDescription(replaceBlank(unsPo.getDescription(), ""));

        data.setParentDataType(FolderDataType.getFolderDataType(unsPo.getParentDataType()).name());

        if (unsPo.getDataType() == Constants.CALCULATION_REAL_TYPE
                || unsPo.getDataType() == Constants.MERGE_TYPE
                || unsPo.getDataType() == Constants.CITING_TYPE) {
            data.setRefers(handleRefer(context, unsPo.getRefers(), unsPo.getDataType()));
        }

        if (unsPo.getDataType() == Constants.MERGE_TYPE || unsPo.getDataType() == Constants.CITING_TYPE) {
            data.setFields(null);
        }
        data.setType(TYPE_FILE);
        return data;
    }

    /**
     * 时序文件
     *
     * @param unsPo
     * @return
     */
    private static ExportImportData wrapFileTimeseriesData(UnsPo unsPo, ExcelExportContext context) {
        Map<Long, UnsPo> templateMap = context.getTemplateMap();
        Map<Long, Set<String>> unsLabelNamesMap = context.getUnsLabelNamesMap();

        FileTimeseriesData data = new FileTimeseriesData();

        data.setAlias(replaceBlank(unsPo.getAlias(), ""));
        data.setDisplayName(replaceBlank(unsPo.getDisplayName(), ""));
        if (unsPo.getModelId() != null) {
            UnsPo template = templateMap.get(unsPo.getModelId());
            data.setTemplateAlias(template != null ? template.getAlias() : "");
        } else {
            data.setTemplateAlias("");
        }
        //data.setNamespace(replaceBlank(unsPo.getPath(), ""));
        data.setName(replaceBlank(unsPo.getName(), ""));

        data.setEnableHistory(gainFlagValue(unsPo.getWithFlags(), "persistence"));
        data.setGenerateDashboard(gainFlagValue(unsPo.getWithFlags(), "dashboard"));
        data.setMockData(gainFlagValue(unsPo.getWithFlags(), "mockData"));
        data.setParentDataType(FolderDataType.getFolderDataType(unsPo.getParentDataType()).name());
        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            data.setLabel(StringUtils.join(labels, ','));
        } else {
            data.setLabel("");
        }

        data.setFields(FieldUtils.filterSystemField(unsPo.getFields()));
        data.setDescription(replaceBlank(unsPo.getDescription(), ""));

        return data;
    }

    /**
     * 关系文件
     *
     * @param unsPo
     * @return
     */
    private static ExportImportData wrapFileRelationData(UnsPo unsPo, ExcelExportContext context) {
        Map<Long, UnsPo> templateMap = context.getTemplateMap();
        Map<Long, Set<String>> unsLabelNamesMap = context.getUnsLabelNamesMap();

        FileRelationData data = new FileRelationData();

        data.setAlias(replaceBlank(unsPo.getAlias(), ""));
        data.setDisplayName(replaceBlank(unsPo.getDisplayName(), ""));
        if (unsPo.getModelId() != null) {
            UnsPo template = templateMap.get(unsPo.getModelId());
            data.setTemplateAlias(template != null ? template.getAlias() : "");
        } else {
            data.setTemplateAlias("");
        }
        //data.setNamespace(replaceBlank(unsPo.getPath(), ""));
        data.setName(replaceBlank(unsPo.getName(), ""));
        data.setEnableHistory(gainFlagValue(unsPo.getWithFlags(), "persistence"));
        data.setGenerateDashboard(gainFlagValue(unsPo.getWithFlags(), "dashboard"));
        data.setMockData(gainFlagValue(unsPo.getWithFlags(), "mockData"));
        data.setParentDataType(FolderDataType.getFolderDataType(unsPo.getParentDataType()).name());
        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            data.setLabel(StringUtils.join(labels, ','));
        } else {
            data.setLabel("");
        }

        data.setFields(FieldUtils.filterSystemField(unsPo.getFields()));
        data.setDescription(replaceBlank(unsPo.getDescription(), ""));

        return data;
    }

    /**
     * 计算文件
     *
     * @param unsPo
     * @return
     */
    private static ExportImportData wrapFileCalculateData(UnsPo unsPo, ExcelExportContext context) {
        Map<Long, UnsPo> templateMap = context.getTemplateMap();
        Map<Long, Set<String>> unsLabelNamesMap = context.getUnsLabelNamesMap();

        FileCalculateData data = new FileCalculateData();

        data.setAlias(replaceBlank(unsPo.getAlias(), ""));
        data.setDisplayName(replaceBlank(unsPo.getDisplayName(), ""));
        //data.setNamespace(replaceBlank(unsPo.getPath(), ""));
        data.setName(replaceBlank(unsPo.getName(), ""));
        data.setEnableHistory(gainFlagValue(unsPo.getWithFlags(), "persistence"));
        data.setGenerateDashboard(gainFlagValue(unsPo.getWithFlags(), "dashboard"));
        if (StringUtils.isNotBlank(unsPo.getExpression())) {
            data.setExpression(unsPo.getExpression());
        } else {
            data.setExpression("");
        }

        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            data.setLabel(StringUtils.join(labels, ','));
        } else {
            data.setLabel("");
        }

        data.setFields(FieldUtils.filterSystemField(unsPo.getFields()));
        data.setDescription(replaceBlank(unsPo.getDescription(), ""));

        data.setRefers(handleRefer(context, unsPo.getRefers(), unsPo.getDataType()));
        data.setParentDataType(FolderDataType.getFolderDataType(unsPo.getParentDataType()).name());
        return data;
    }

    /**
     * 聚合文件
     *
     * @param unsPo
     * @return
     */
    private static ExportImportData wrapFileAggregationData(UnsPo unsPo, ExcelExportContext context) {
        Map<Long, Set<String>> unsLabelNamesMap = context.getUnsLabelNamesMap();

        FileAggregationData data = new FileAggregationData();

        data.setAlias(replaceBlank(unsPo.getAlias(), ""));
        data.setDisplayName(replaceBlank(unsPo.getDisplayName(), ""));
        //data.setNamespace(replaceBlank(unsPo.getPath(), ""));
        data.setName(replaceBlank(unsPo.getName(), ""));
        data.setEnableHistory(gainFlagValue(unsPo.getWithFlags(), "persistence"));
        data.setGenerateDashboard(gainFlagValue(unsPo.getWithFlags(), "dashboard"));
        data.setParentDataType(FolderDataType.getFolderDataType(unsPo.getParentDataType()).name());
        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            data.setLabel(StringUtils.join(labels, ','));
        } else {
            data.setLabel("");
        }

        String frequencyStr = "";
        String protocol = unsPo.getProtocol();
        if (JSONObject.isValidObject(protocol)) {
            JSONObject jsonObject = JSONObject.parseObject(protocol);
            Object frequency =jsonObject.get("frequency");
            if (frequency != null) {
                frequencyStr = frequency.toString();
            }
        }
        data.setFrequency(frequencyStr);

        data.setDescription(replaceBlank(unsPo.getDescription(), ""));

        data.setRefers(handleRefer(context, unsPo.getRefers(), unsPo.getDataType()));

        return data;
    }

    /**
     * 引用文件
     *
     * @param unsPo
     * @return
     */
    private static ExportImportData wrapFileReferenceData(UnsPo unsPo, ExcelExportContext context) {
        Map<Long, Set<String>> unsLabelNamesMap = context.getUnsLabelNamesMap();

        FileReferenceData data = new FileReferenceData();

        data.setAlias(replaceBlank(unsPo.getAlias(), ""));
        data.setDisplayName(replaceBlank(unsPo.getDisplayName(), ""));
        //data.setNamespace(replaceBlank(unsPo.getPath(), ""));
        data.setName(replaceBlank(unsPo.getName(), ""));
//        data.setEnableHistory(gainFlagValue(unsPo.getWithFlags(), "persistence"));
//        data.setGenerateDashboard(gainFlagValue(unsPo.getWithFlags(), "dashboard"));

        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            data.setLabel(StringUtils.join(labels, ','));
        } else {
            data.setLabel("");
        }

        data.setDescription(replaceBlank(unsPo.getDescription(), ""));

        data.setRefers(handleRefer(context, unsPo.getRefers(), unsPo.getDataType()));
        data.setParentDataType(FolderDataType.getFolderDataType(unsPo.getParentDataType()).name());
        return data;
    }

    /**
     * JSONB文件
     * @param unsPo
     * @param context
     * @return
     */
    private static ExportImportData wrapFileJsonbData(UnsPo unsPo, ExcelExportContext context) {
        Map<Long, Set<String>> unsLabelNamesMap = context.getUnsLabelNamesMap();

        FileJsonbData data = new FileJsonbData();

        data.setAlias(replaceBlank(unsPo.getAlias(), ""));
        data.setDisplayName(replaceBlank(unsPo.getDisplayName(), ""));
        //data.setNamespace(replaceBlank(unsPo.getPath(), ""));
        data.setName(replaceBlank(unsPo.getName(), ""));

        data.setEnableHistory(gainFlagValue(unsPo.getWithFlags(), "persistence"));
        data.setGenerateDashboard(gainFlagValue(unsPo.getWithFlags(), "dashboard"));
//        data.setMockData("FALSE");
        data.setParentDataType(FolderDataType.getFolderDataType(unsPo.getParentDataType()).name());
        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            data.setLabel(StringUtils.join(labels, ','));
        } else {
            data.setLabel("");
        }

//        data.setFields(field(new FieldDefine[]{new FieldDefine("json", FieldType.STRING)}));
        data.setDescription(replaceBlank(unsPo.getDescription(), ""));

        return data;
    }

    private static String field(FieldDefine[] fs) {
        if (ArrayUtils.isNotEmpty(fs)) {
            FieldDefine[] field = Arrays.stream(fs).filter(f -> !f.isSystemField()).toArray(FieldDefine[]::new);
            if (ArrayUtils.isNotEmpty(field)) {
                return JsonUtil.toJson(field);
            }
        }
        return "";
    }

    private static String handleRefer(ExcelExportContext context, InstanceField[] refers, int dataType) {
        JSONArray jsonArray = new JSONArray();
        if (ArrayUtils.isNotEmpty(refers)) {
            for (InstanceField field : refers) {
                UnsPo ref = null;
                if (field.getId() != null) {
                    ref = context.getExportFileById(field.getId());
                } else if (StringUtils.isNotBlank(field.getAlias())) {
                    ref = context.getExportFileByAlias(field.getAlias());
                } else if (StringUtils.isNotBlank(field.getPath())) {
                    ref = context.getExportFileByPath(field.getPath());
                }

                if (ref != null) {
                    JSONObject jsonObject = new JSONObject();
                    if (dataType == Constants.CALCULATION_REAL_TYPE) {
                        jsonObject.put("field", field.getField());
                        jsonObject.put("path", ref.getPath());
                        jsonObject.put("alias", ref.getAlias());
                    } else if (dataType == Constants.MERGE_TYPE || dataType == Constants.CITING_TYPE) {
                        jsonObject.put("path", ref.getPath());
                        jsonObject.put("alias", ref.getAlias());
                    }
//                    if (field.getUts() != null) {
//                        jsonObject.put("uts", field.getUts());
//                    }

                    jsonArray.add(jsonObject);
                }
            }
        }
        return jsonArray.toString();
    }
}
