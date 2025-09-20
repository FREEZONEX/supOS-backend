package com.supos.uns.service.exportimport.excel;

import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.supos.common.Constants;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.utils.FileUtils;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.UnsExcelService;
import com.supos.uns.service.UnsManagerService;
import com.supos.uns.service.exportimport.core.DataExporter;
import com.supos.uns.service.exportimport.core.ExcelExportContext;
import com.supos.uns.service.exportimport.core.ExportImportHelper;
import com.supos.uns.service.exportimport.core.entity.ExportImportData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelDataExporter
 * @date 2025/5/10 17:57
 */
@Slf4j
public class ExcelDataExporter extends DataExporter {

    private UnsManagerService unsManagerService;
    private UnsExcelService unsExcelService;

    public ExcelDataExporter(UnsManagerService unsManagerService, UnsExcelService unsExcelService) {
        this.unsManagerService = unsManagerService;
        this.unsExcelService = unsExcelService;
    }

    @Override
    public String exportData(ExcelExportContext context) {
        try {
            String templatePath = Constants.EXCEL_TEMPLATE_PATH;
            String language = context.getLanguage();
            if (language != null) {
                if (StringUtils.containsIgnoreCase(language, "zh")) {
                    templatePath = Constants.EXCEL_TEMPLATE_ZH_PATH;
                }
            }

            String datePath = currentTime();
            String path = String.format("%s%s%s", Constants.EXCEL_ROOT, datePath, "/export/namespace-" + datePath + ".xlsx");
            String targetPath = String.format("%s%s", FileUtils.getFileRootPath(), path);
            //String targetPath = "./export/xx.xlsx";
            File exportFile = FileUtil.touch(targetPath);
            log.info("create export file:{}", exportFile.getAbsolutePath());

            ExcelWriter excelWriter = EasyExcel.write(targetPath).withTemplate(new ClassPathResource(templatePath).getInputStream()).build();
            //  写说明页
            unsExcelService.writeExplanationRow(excelWriter);

            ExcelTypeEnum activeExcelType = ExcelTypeEnum.Explanation;
            // 导出模板
            if (MapUtils.isNotEmpty(context.getTemplateMap())) {
                List<ExportImportData> templateDatas = context.getTemplateMap().values().stream().map(template -> ExportImportHelper.wrapData(ExcelTypeEnum.Template, template, context)).collect(Collectors.toList());
                writeRow(excelWriter, ExcelTypeEnum.Template, templateDatas);
                activeExcelType = getActiveExcelType(activeExcelType, ExcelTypeEnum.Template);
            }

            // 导出标签
            Map<Long, UnsLabelPo> labels = context.getLabelMap();
            if (MapUtils.isNotEmpty(labels)) {
                List<ExportImportData> labelDatas = labels.values().stream().map(label -> ExportImportHelper.wrapLabelData(label)).collect(Collectors.toList());
                writeRow(excelWriter, ExcelTypeEnum.Label, labelDatas);
                activeExcelType = getActiveExcelType(activeExcelType, ExcelTypeEnum.Label);
            }


            Collection<UnsPo> exportFileList = context.getAllExportFile();
            if (CollectionUtils.isNotEmpty(exportFileList)) {
                // 导出文件
                Map<ExcelTypeEnum, List<ExportImportData>> fileDataMap = new HashMap<>();
                for (UnsPo file : exportFileList) {
                    ExcelTypeEnum excelTypeEnum = ExcelTypeEnum.getByDataType(file.getDataType());
                    fileDataMap.computeIfAbsent(excelTypeEnum, k -> new LinkedList<>()).add(ExportImportHelper.wrapData(excelTypeEnum, file, context));
                }
                for (Map.Entry<ExcelTypeEnum, List<ExportImportData>> e : fileDataMap.entrySet()) {
                    writeRow(excelWriter, e.getKey(), e.getValue());
                    activeExcelType = getActiveExcelType(activeExcelType, e.getKey());
                }
            }
            Collection<UnsPo> exportFolderList = context.getAllExportFolder();
            // 导出文件夹
            if (CollectionUtils.isNotEmpty(exportFolderList)) {
                List<ExportImportData> folderDatas = exportFolderList.stream().map(folder -> ExportImportHelper.wrapData(ExcelTypeEnum.Folder, folder, context)).collect(Collectors.toList());
                writeRow(excelWriter, ExcelTypeEnum.Folder, folderDatas);
                activeExcelType = getActiveExcelType(activeExcelType, ExcelTypeEnum.Folder);
            }

            excelWriter.writeContext().getWorkbook().setActiveSheet(activeExcelType.getIndex());

            excelWriter.finish();
            log.info("export success:{}", targetPath);
            return path;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ExcelTypeEnum getActiveExcelType(ExcelTypeEnum activeExcelType, ExcelTypeEnum currentExcelType) {
        if (activeExcelType == ExcelTypeEnum.Explanation) {
            return currentExcelType;
        }
        return activeExcelType;
    }

    private void writeRow(ExcelWriter excelWriter, ExcelTypeEnum excelType, List<ExportImportData> dataList) {
        WriteSheet writeSheet = EasyExcel.writerSheet().relativeHeadRowIndex(0).sheetNo(excelType.getIndex()).build();

        excelWriter.write(dataList, writeSheet);
    }
}
