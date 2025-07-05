package com.supos.uns.service.exportimport.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.exception.BuzException;
import com.supos.uns.service.UnsAddService;
import com.supos.uns.service.UnsLabelService;
import com.supos.uns.service.UnsManagerService;
import com.supos.uns.service.UnsTemplateService;
import com.supos.uns.service.exportimport.core.DataImporter;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.data.*;
import com.supos.uns.service.exportimport.json.data.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * TODO 后续性能优化和内存优化需考虑使用流式处理
 * @author sunlifang
 * @version 1.0
 * @description: JsonDataImporter
 * @date 2025/5/10 13:05
 */
@Slf4j
public class JsonDataImporter extends DataImporter {

    private long batch = 2000;
    private JsonWraper jsonWraper;

    public JsonDataImporter(ExcelImportContext context, UnsManagerService unsManagerService, UnsLabelService unsLabelService,
                            UnsTemplateService unsTemplateService, UnsAddService unsAddService) {
        super(context, unsManagerService, unsLabelService, unsTemplateService, unsAddService);
    }

    @Override
    public void importData(File file) {
        try {
            JsonMapper jsonMapper = new JsonMapper();
            jsonWraper = jsonMapper.readValue(file, JsonWraper.class);
        } catch (Exception e) {
            log.error("解析json文件失败", e);
            throw new BuzException("uns.import.json.error");
        }
        try {
            handleTemplate(jsonWraper);
            handleLabel(jsonWraper);
            handleFolder(jsonWraper);
            handleFile(jsonWraper);

            log.info("import running time:{}s", getStopWatch().getTotalTimeSeconds());
            log.info(getStopWatch().prettyPrint());
        } catch (Exception e) {
            log.error("导入失败", e);
            throw new BuzException("uns.import.error");
        }
    }

    @Override
    public void writeError(File srcfile, File outFile) {
        try {
            JsonFactory factory = new JsonMapper().getFactory();
            JsonGenerator jsonGenerator = factory.createGenerator(outFile, JsonEncoding.UTF8);
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeStartObject();

            String error = null;
            // 导出模板
            jsonGenerator.writeFieldName(ExcelTypeEnum.Template.getCode());
            jsonGenerator.writeStartArray();
            List<TemplateData> templateDataList = jsonWraper.getTemplateDataList();
            if (CollectionUtils.isNotEmpty(templateDataList)) {
                TemplateData templateData = null;
                for (int i = 0; i < templateDataList.size(); i++) {
                    templateData = templateDataList.get(i);
                    templateData.setError(getErrorMsg(ExcelTypeEnum.Template, i));
                    jsonGenerator.writePOJO(templateData);
                }
            }
            jsonGenerator.writeEndArray();

            // 导出标签
            jsonGenerator.writeFieldName(ExcelTypeEnum.Label.getCode());
            jsonGenerator.writeStartArray();
            List<LabelData> labelDataList = jsonWraper.getLabelDataList();
            if (CollectionUtils.isNotEmpty(labelDataList)) {
                LabelData label = null;
                for (int i = 0; i < labelDataList.size(); i++) {
                    label = labelDataList.get(i);
                    label.setError(getErrorMsg(ExcelTypeEnum.Label, i));
                    jsonGenerator.writePOJO(label);
                }
            }
            jsonGenerator.writeEndArray();

            // 导出文件夹
            jsonGenerator.writeFieldName(ExcelTypeEnum.Folder.getCode());
            jsonGenerator.writeStartArray();
            List<FolderData> folderDataDataList = jsonWraper.getFolderDataDataList();
            if (CollectionUtils.isNotEmpty(folderDataDataList)) {
                FolderData folderData = null;
                for (int i = 0; i < folderDataDataList.size(); i++) {
                    folderData = folderDataDataList.get(i);
                    folderData.setError(getErrorMsg(ExcelTypeEnum.Folder, i));
                    jsonGenerator.writePOJO(folderData);
                }
            }
            jsonGenerator.writeEndArray();

            jsonGenerator.writeFieldName(ExcelTypeEnum.FILE_TIMESERIES.getCode());
            jsonGenerator.writeStartArray();
            List<FileTimeseries> fileTimeseriesDataList = jsonWraper.getFileTimeseriesDataList();
            if (CollectionUtils.isNotEmpty(fileTimeseriesDataList)) {
                FileTimeseries fileTimeseries = null;
                for (int i = 0; i < fileTimeseriesDataList.size(); i++) {
                    fileTimeseries = fileTimeseriesDataList.get(i);
                    fileTimeseries.setError(getErrorMsg(ExcelTypeEnum.FILE_TIMESERIES, i));
                    jsonGenerator.writePOJO(fileTimeseries);
                }
            }
            jsonGenerator.writeEndArray();

            jsonGenerator.writeFieldName(ExcelTypeEnum.FILE_RELATION.getCode());
            jsonGenerator.writeStartArray();
            List<FileRelation> fileRelationDataList = jsonWraper.getFileRelationDataList();
            if (CollectionUtils.isNotEmpty(fileRelationDataList)) {
                FileRelation fileRelation = null;
                for (int i = 0; i < fileRelationDataList.size(); i++) {
                    fileRelation = fileRelationDataList.get(i);
                    fileRelation.setError(getErrorMsg(ExcelTypeEnum.FILE_RELATION, i));
                    jsonGenerator.writePOJO(fileRelation);
                }
            }
            jsonGenerator.writeEndArray();

            jsonGenerator.writeFieldName(ExcelTypeEnum.FILE_CALCULATE.getCode());
            jsonGenerator.writeStartArray();
            List<FileCalculate> fileCalculateDataList = jsonWraper.getFileCalculateDataList();
            if (CollectionUtils.isNotEmpty(fileCalculateDataList)) {
                FileCalculate fileCalculate = null;
                for (int i = 0; i < fileCalculateDataList.size(); i++) {
                    fileCalculate = fileCalculateDataList.get(i);
                    fileCalculate.setError(getErrorMsg(ExcelTypeEnum.FILE_CALCULATE, i));
                    jsonGenerator.writePOJO(fileCalculate);
                }
            }
            jsonGenerator.writeEndArray();

            jsonGenerator.writeFieldName(ExcelTypeEnum.FILE_AGGREGATION.getCode());
            jsonGenerator.writeStartArray();
            List<FileAggregation> fileAggregationDataList = jsonWraper.getFileAggregationDataList();
            if (CollectionUtils.isNotEmpty(fileAggregationDataList)) {
                FileAggregation fileAggregation = null;
                for (int i = 0; i < fileAggregationDataList.size(); i++) {
                    fileAggregation = fileAggregationDataList.get(i);
                    fileAggregation.setError(getErrorMsg(ExcelTypeEnum.FILE_AGGREGATION, i));
                    jsonGenerator.writePOJO(fileAggregation);
                }
            }
            jsonGenerator.writeEndArray();

            jsonGenerator.writeFieldName(ExcelTypeEnum.FILE_REFERENCE.getCode());
            jsonGenerator.writeStartArray();
            List<FileReference> fileReferenceDataList = jsonWraper.getFileReferenceDataList();
            if (CollectionUtils.isNotEmpty(fileReferenceDataList)) {
                FileReference fileReference = null;
                for (int i = 0; i < fileReferenceDataList.size(); i++) {
                    fileReference = fileReferenceDataList.get(i);
                    fileReference.setError(getErrorMsg(ExcelTypeEnum.FILE_REFERENCE, i));
                    jsonGenerator.writePOJO(fileReference);
                }
            }
            jsonGenerator.writeEndArray();

            jsonGenerator.writeEndObject();
            jsonGenerator.close();
        } catch (Exception e) {
            log.error("导入失败", e);
            throw new BuzException(e.getMessage());
        }
    }
    private String getErrorMsg(ExcelTypeEnum excelTypeEnum, int rowIndex) {
        Map<Integer, String> subErrorMap = getContext().getError().get(excelTypeEnum.getIndex());
        if (subErrorMap != null) {
            return subErrorMap.get(rowIndex);
        }
        return null;
    }

    private void handleTemplate(JsonWraper jsonWraper) {
        List<TemplateData> templateDataList = jsonWraper.getTemplateDataList();
        if (CollectionUtils.isNotEmpty(templateDataList)) {
            for(int i = 0; i < templateDataList.size(); i++) {
                getParser(ExcelTypeEnum.Template).parseJson(ExcelTypeEnum.Template.getIndex(), i,  templateDataList.get(i), getContext());
                if (i % batch == 0) {
                    doImport(ExcelTypeEnum.Template);
                }
            }
            doImport(ExcelTypeEnum.Template);
        }
    }

    private void handleLabel(JsonWraper jsonWraper) {
        List<LabelData> labelDataList = jsonWraper.getLabelDataList();
        if (CollectionUtils.isNotEmpty(labelDataList)) {
            for(int i = 0; i < labelDataList.size(); i++) {
                getParser(ExcelTypeEnum.Label).parseJson(ExcelTypeEnum.Label.getIndex(), i,  labelDataList.get(i), getContext());
                if (i % batch == 0) {
                    doImport(ExcelTypeEnum.Label);
                }
            }
            doImport(ExcelTypeEnum.Label);
        }
    }

    private void handleFolder(JsonWraper jsonWraper) {
        List<FolderData> folderDataDataList = jsonWraper.getFolderDataDataList();
        if (CollectionUtils.isNotEmpty(folderDataDataList)) {
            for(int i = 0; i < folderDataDataList.size(); i++) {
                getParser(ExcelTypeEnum.Folder).parseJson(ExcelTypeEnum.Folder.getIndex(), i,  folderDataDataList.get(i), getContext());
                if (i % batch == 0) {
                    doImport(ExcelTypeEnum.Folder);
                }
            }
            doImport(ExcelTypeEnum.Folder);
        }
    }

    private void handleFile(JsonWraper jsonWraper) {
        List<ExcelTypeEnum> excelTypes =ExcelTypeEnum.listFile();

        for(ExcelTypeEnum excelType : excelTypes) {
            List<?  extends ExportImportData> dataList = null;
            switch (excelType) {
                case FILE_TIMESERIES:
                    dataList = jsonWraper.getFileTimeseriesDataList();break;
                case FILE_RELATION:
                    dataList = jsonWraper.getFileRelationDataList();break;
                case FILE_CALCULATE:
                    dataList = jsonWraper.getFileCalculateDataList();break;
                case FILE_AGGREGATION:
                    dataList = jsonWraper.getFileAggregationDataList();break;
                case FILE_REFERENCE:
                    dataList = jsonWraper.getFileReferenceDataList();break;
                default:
                    break;
            }

            if (CollectionUtils.isNotEmpty(dataList)) {
                for(int i = 0; i < dataList.size(); i++) {
                    getParser(excelType).parseJson(excelType.getIndex(), i,  dataList.get(i), getContext());
                    if (i % batch == 0) {
                        doImport(excelType);
                    }
                }
                doImport(excelType);
            }
        }
    }
}
