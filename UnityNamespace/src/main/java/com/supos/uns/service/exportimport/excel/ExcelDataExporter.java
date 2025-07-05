package com.supos.uns.service.exportimport.excel;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.supos.common.Constants;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.service.UnsExcelService;
import com.supos.uns.service.UnsManagerService;
import com.supos.uns.service.exportimport.core.DataExporter;
import com.supos.uns.service.exportimport.core.ExcelExportContext;
import com.supos.uns.service.exportimport.core.ExportNode;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import com.supos.uns.util.ExportImportUtil;
import com.supos.uns.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.core.io.ClassPathResource;

import java.util.Date;
import java.util.List;
import java.util.Map;
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
            String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String path = String.format("%s%s%s", Constants.EXCEL_ROOT, datePath, Constants.EXCEL_OUT_PATH);
            String targetPath = String.format("%s%s", FileUtils.getFileRootPath(), path);
            FileUtil.touch(targetPath);

            ExcelWriter excelWriter = EasyExcel.write(targetPath).withTemplate(new ClassPathResource(Constants.EXCEL_TEMPLATE_PATH).getInputStream()).build();
            //  写说明页
            unsExcelService.writeExplanationRow(excelWriter);

            ExcelTypeEnum activeExcelType = ExcelTypeEnum.Explanation;
            // 导出模板
            if (MapUtils.isNotEmpty(context.getTemplateMap())) {
                List<ExportImportData> templateDatas = context.getTemplateMap().values().stream().map(template -> ExportImportUtil.createRow(template, context).getExportImportData()).collect(Collectors.toList());
                writeRow(excelWriter, ExcelTypeEnum.Template, templateDatas);
                activeExcelType = getActiveExcelType(activeExcelType, ExcelTypeEnum.Template);
            }

            // 导出标签
            Map<Long, UnsLabelPo> labels = context.getLabelMap();
            if (MapUtils.isNotEmpty(labels)) {
                List<ExportImportData> labelDatas = labels.values().stream().map(label -> ExportImportUtil.createRow(label).getExportImportData()).collect(Collectors.toList());
                writeRow(excelWriter, ExcelTypeEnum.Label, labelDatas);
                activeExcelType = getActiveExcelType(activeExcelType, ExcelTypeEnum.Label);
            }


            List<ExportNode> exportFileList = context.getExportFileList();
            if (CollectionUtils.isNotEmpty(exportFileList)) {
                // 导出文件
                Map<ExcelTypeEnum, List<ExportImportUtil.RowWrapper>> rowWrapperMap = exportFileList.stream().map(file -> ExportImportUtil.createRow(file.getUnsPo(), context)).collect(Collectors.groupingBy(ExportImportUtil.RowWrapper::getExcelType));
                for (Map.Entry<ExcelTypeEnum, List<ExportImportUtil.RowWrapper>> e : rowWrapperMap.entrySet()) {
                    if (CollectionUtils.isNotEmpty(e.getValue())) {
                        writeRow(excelWriter, e.getKey(), e.getValue().stream().map(ExportImportUtil.RowWrapper::getExportImportData).collect(Collectors.toList()));
                        activeExcelType = getActiveExcelType(activeExcelType, e.getKey());
                    }
                }
            }
            List<ExportNode> exportFolderList = context.getExportFolderList();
            // 导出文件夹
            if (CollectionUtils.isNotEmpty(exportFolderList)) {
                List<ExportImportData> folderDatas = exportFolderList.stream().map(folder -> ExportImportUtil.createRow(folder.getUnsPo(), context).getExportImportData()).collect(Collectors.toList());
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
