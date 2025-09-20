package com.supos.uns.service.exportimport.cjson;

import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.supos.common.Constants;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.utils.FileUtils;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.exportimport.core.DataExporter;
import com.supos.uns.service.exportimport.core.ExcelExportContext;
import com.supos.uns.service.exportimport.core.ExportImportHelper;
import com.supos.uns.service.exportimport.core.entity.ExportImportData;
import com.supos.uns.service.exportimport.core.entity.FolderData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import java.io.File;
import java.util.*;

/**
 * @author sunlifang
 * @version 1.0
 * @description: JsonDataExporter
 * @date 2025/5/10 17:57
 */
@Slf4j
public class ComplexJsonDataExporter extends DataExporter {

    public ComplexJsonDataExporter() {

    }

    @Override
    public String exportData(ExcelExportContext context) {
        try {
            String datePath = currentTime();
            String path = String.format("%s%s%s", Constants.EXCEL_ROOT, datePath, "/export/namespace-" + datePath + ".json");
            String targetPath = String.format("%s%s", FileUtils.getFileRootPath(), path);
            //String targetPath = "./export/xx.json";
            File exportFile = FileUtil.touch(targetPath);
            log.info("create export file:{}", exportFile.getAbsolutePath());

            JsonMapper jsonMapper = new JsonMapper();
            jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            JsonFactory factory = jsonMapper.getFactory();
            JsonGenerator jsonGenerator = factory.createGenerator(exportFile, JsonEncoding.UTF8);
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeStartObject();
            // 导出模板
            jsonGenerator.writeFieldName(ExcelTypeEnum.Template.getCode());
            jsonGenerator.writeStartArray();
            if (MapUtils.isNotEmpty(context.getTemplateMap())) {
                for (UnsPo template : context.getTemplateMap().values()) {
                    jsonGenerator.writePOJO(ExportImportHelper.wrapData(ExcelTypeEnum.Template, template, context));
                }
            }
            jsonGenerator.writeEndArray();

            // 导出标签
            jsonGenerator.writeFieldName(ExcelTypeEnum.Label.getCode());
            jsonGenerator.writeStartArray();
            Map<Long, UnsLabelPo> labels = context.getLabelMap();
            if (MapUtils.isNotEmpty(labels)) {
                for (UnsLabelPo label : labels.values()) {
                    jsonGenerator.writePOJO(ExportImportHelper.wrapLabelData(label));
                }
            }
            jsonGenerator.writeEndArray();

            // 导出UNS
            List<ExportImportData> uns = wrapComplexJson(context.getAllExportFolder(), context.getAllExportFile(), context);

            // 导出文件夹
            jsonGenerator.writeFieldName("UNS");
            jsonGenerator.writeObject(uns);

            jsonGenerator.writeEndObject();
            jsonGenerator.close();
            log.info("export success:{}", targetPath);
            return path;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<ExportImportData> wrapComplexJson(Collection<UnsPo> exportFolderList, Collection<UnsPo> exportFileList, ExcelExportContext context) {
        // 按上下级归类
        List<ExportImportData> root = new LinkedList<>();
        Map<String, ExportImportData> tempMap = new HashMap<>();

        for (UnsPo folderNode : exportFolderList) {
            ExportImportData tempData = ExportImportHelper.wrapData(ExcelTypeEnum.Folder, folderNode, context);
            tempMap.put(folderNode.getAlias(), tempData);
        }

        for (UnsPo folderNode : exportFolderList) {
            if (folderNode.getParentAlias() == null) {
                root.add(tempMap.get(folderNode.getAlias()));
            } else {
                ExportImportData parentData = tempMap.get(folderNode.getParentAlias());
                ((FolderData)parentData).addChild(tempMap.get(folderNode.getAlias()));
            }
        }

        for (UnsPo fileNode : exportFileList) {
            ExportImportData data = ExportImportHelper.wrapData(ExcelTypeEnum.File, fileNode, context);
            if (fileNode.getParentAlias() == null) {
                root.add(data);
            } else {
                ExportImportData parentData = tempMap.get(fileNode.getParentAlias());


                ((FolderData)parentData).addChild(data);
            }
        }

        return root;
    }
}
