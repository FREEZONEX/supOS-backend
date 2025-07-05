package com.supos.uns.service.exportimport.json;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.supos.common.Constants;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.UnsManagerService;
import com.supos.uns.service.exportimport.core.DataExporter;
import com.supos.uns.service.exportimport.core.ExcelExportContext;
import com.supos.uns.service.exportimport.core.ExportNode;
import com.supos.uns.util.ExportImportUtil;
import com.supos.uns.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: JsonDataExporter
 * @date 2025/5/10 17:57
 */
@Slf4j
public class JsonDataExporter extends DataExporter {

    private UnsManagerService unsManagerService;

    public JsonDataExporter(UnsManagerService unsManagerService) {
        this.unsManagerService = unsManagerService;
    }

    @Override
    public String exportData(ExcelExportContext context) {
        try {
            String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String path = String.format("%s%s%s", Constants.EXCEL_ROOT, datePath, Constants.JSON_OUT_PATH);
            String targetPath = String.format("%s%s", FileUtils.getFileRootPath(), path);
            FileUtil.touch(targetPath);
            JsonFactory factory = new JsonMapper().getFactory();
            JsonGenerator jsonGenerator = factory.createGenerator(new File(targetPath), JsonEncoding.UTF8);
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeStartObject();
            // 导出模板
            jsonGenerator.writeFieldName(ExcelTypeEnum.Template.getCode());
            jsonGenerator.writeStartArray();
            if (MapUtils.isNotEmpty(context.getTemplateMap())) {
                for (UnsPo template : context.getTemplateMap().values()) {
                    jsonGenerator.writePOJO(ExportImportUtil.createRow(template, context).getExportImportData());
                }
            }
            jsonGenerator.writeEndArray();

            // 导出标签
            jsonGenerator.writeFieldName(ExcelTypeEnum.Label.getCode());
            jsonGenerator.writeStartArray();
            Map<Long, UnsLabelPo> labels = context.getLabelMap();
            if (MapUtils.isNotEmpty(labels)) {
                for (UnsLabelPo label : labels.values()) {
                    jsonGenerator.writePOJO(ExportImportUtil.createRow(label).getExportImportData());
                }
            }
            jsonGenerator.writeEndArray();

            // 导出文件夹
            jsonGenerator.writeFieldName(ExcelTypeEnum.Folder.getCode());
            jsonGenerator.writeStartArray();
            List<ExportNode> exportFolderList = context.getExportFolderList();
            if (CollectionUtils.isNotEmpty(exportFolderList)) {
                for (ExportNode exportNode : exportFolderList) {
                    jsonGenerator.writePOJO(ExportImportUtil.createRow(exportNode.getUnsPo(), context).getExportImportData());
                }
            }
            jsonGenerator.writeEndArray();

            //if (CollectionUtils.isNotEmpty(exportFileList)) {
                // 导出文件
            List<ExportNode> exportFileList = context.getExportFileList();
            Map<ExcelTypeEnum, List<ExportImportUtil.RowWrapper>> rowWrapperMap = exportFileList.stream().map(file -> ExportImportUtil.createRow(file.getUnsPo(), context)).collect(Collectors.groupingBy(ExportImportUtil.RowWrapper::getExcelType));

            List<ExcelTypeEnum> fileTypes = ExcelTypeEnum.listFile();
            for(ExcelTypeEnum excelType : fileTypes) {
                jsonGenerator.writeFieldName(excelType.getCode());
                jsonGenerator.writeStartArray();
                List<ExportImportUtil.RowWrapper> rowWrappers = rowWrapperMap.get(excelType);
                if (CollectionUtils.isNotEmpty(rowWrappers)) {
                    for (ExportImportUtil.RowWrapper rowWrapper : rowWrappers) {
                        jsonGenerator.writePOJO(rowWrapper.getExportImportData());
                    }
                }
                jsonGenerator.writeEndArray();
            }
            //}

            jsonGenerator.writeEndObject();
            jsonGenerator.close();
            log.info("export success:{}", targetPath);
            return path;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
