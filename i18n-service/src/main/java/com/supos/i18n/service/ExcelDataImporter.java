package com.supos.i18n.service;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.supos.common.RunningStatus;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.I18nUtils;
import com.supos.i18n.service.excel.ExcelImportContext;
import com.supos.i18n.service.excel.ExcelRowErrorHandler;
import com.supos.i18n.service.excel.ExcelWriteHandler;
import com.supos.i18n.service.excel.ImportExportHelper;
import com.supos.i18n.service.excel.entity.LanguageData;
import com.supos.i18n.service.excel.entity.ModuleData;
import com.supos.i18n.service.excel.entity.ResourceData;
import com.supos.i18n.service.excel.parser.LanguageParser;
import com.supos.i18n.service.excel.parser.ModuleParser;
import com.supos.i18n.service.excel.parser.ResourceParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelDataImporter
 * @date 2025/5/10 13:04
 */
@Slf4j
public class ExcelDataImporter {

    private ExcelImportContext importContext;
    private I18nManagerService i18nManagerService;

    public ExcelDataImporter(ExcelImportContext importContext, I18nManagerService i18nManagerService) {
        this.importContext = importContext;
        this.i18nManagerService = i18nManagerService;
    }

    public void importData(File file) {
        // 读取excel进行处理
        String version = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        log.info(file.getAbsolutePath());
        ExcelReader excelReader = EasyExcel.read(file).ignoreEmptyRow(false)
                .registerReadListener(new I18nExcelRowHandler(importContext)).build();

        excelReader.readAll();
        excelReader.finish();

        for (String moduleCode : importContext.getActuallyHandleModuleCodes()) {
            i18nManagerService.saveVersion(moduleCode, version);
        }
    }

    public void writeError(File srcfile, File outFile) {
        try {
            // 归类整理错误信息
            Map<Integer, Map<Integer, String>> dataError = new HashMap<>();
            for (Map.Entry<String, String> entry : importContext.getErrorMap().entrySet()) {
                if (entry.getKey().contains("-")) {
                    String[] keyArr = entry.getKey().split("-");
                    Map<Integer, String> subError = dataError.computeIfAbsent(Integer.valueOf(keyArr[0]), k -> new HashMap<>());
                    subError.put(Integer.valueOf(keyArr[1]), entry.getValue());
                }
            }

            ExcelWriter excelWriter = EasyExcel.write(outFile).build();

            ExcelReader excelReader =EasyExcel.read(srcfile).ignoreEmptyRow(false).build();

            List<String> sheetNames = ImportExportHelper.getSheet();
            ReadSheet[] readSheets = new ReadSheet[sheetNames.size()];
            for (int i = 0; i < sheetNames.size(); i++) {
                WriteSheet writeSheet = EasyExcel.writerSheet(i).registerWriteHandler(new ExcelWriteHandler(dataError)).build();
                ReadSheet readSheet =
                        EasyExcel.readSheet(i).sheetName(sheetNames.get(i)).registerReadListener(new ExcelRowErrorHandler(excelWriter, writeSheet, dataError)).build();
                readSheets[i] = readSheet;
            }
            excelReader.read(readSheets);
            excelReader.finish();

            //excelWriter.writeContext().getWorkbook().setActiveSheet(getContext().getActiveExcelType().getIndex());

            excelWriter.finish();
        } catch (Throwable e) {
            log.error("导入失败", e);
            throw new BuzException(e.getMessage());
        }
    }

    /**
     * 自定义excel处理器
     */
    class I18nExcelRowHandler extends AnalysisEventListener<Map<Integer, Object>> {

        private LanguageParser languageParser = new LanguageParser();
        private ModuleParser moduleParser = new ModuleParser();
        private ResourceParser resourceParser = new ResourceParser();

        private ExcelImportContext importContext;

        private String currentSheetName;
        private Integer currentSheetNo;
        private Map<String, Integer> currentHeadMap;

        public I18nExcelRowHandler(ExcelImportContext importContext) {
            this.importContext = importContext;
        }

        // 处理表头
        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            if (importContext.isShoutDown()) {
                return;
            }
            // 说明页不用管表头
            currentSheetName = context.readSheetHolder().getSheetName();
            currentSheetNo = context.readSheetHolder().getSheetNo();
            if (ImportExportHelper.SHEET_EXPLANATION.equals(currentSheetName)) {
                return;
            }
            // 表头
            currentHeadMap = new HashMap<>();
            for (Map.Entry<Integer, String> e : headMap.entrySet()) {
                currentHeadMap.put(e.getValue(), e.getKey());
            }
        }

        // 行数据
        @Override
        public void invoke(Map<Integer, Object> data, AnalysisContext context) {
            if (importContext.isShoutDown()) {
                return;
            }
            Integer rowIndex = context.readRowHolder().getRowIndex();

            String flagNo = String.format("%d-%d", currentSheetNo, rowIndex);
            if (ImportExportHelper.SHEET_EXPLANATION.equals(currentSheetName)) {
                // 说明页直接跳过
                return;
            } else if (ImportExportHelper.SHEET_LANGUAGE.equals(currentSheetName)) {
                // 解析语言，出错直接中断
                boolean result = languageParser.parse(flagNo, currentHeadMap, data, importContext);
                if (!result) {
                    importContext.setShoutDown(true);
                }
            } else if (ImportExportHelper.SHEET_MODULE.equals(currentSheetName)) {
                moduleParser.parse(flagNo, currentHeadMap, data, importContext);
            } else if (ImportExportHelper.SHEET_RESOURCE.equals(currentSheetName)) {
                resourceParser.parse(flagNo, currentHeadMap, data, importContext);
            }

        }

        // 一页读取完成
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (importContext.isShoutDown()) {
                return;
            }
            if (ImportExportHelper.SHEET_EXPLANATION.equals(currentSheetName)) {
                // 说明页直接跳过
                return;
            } else if (ImportExportHelper.SHEET_LANGUAGE.equals(currentSheetName)) {
                saveLanguage();
            } else if (ImportExportHelper.SHEET_MODULE.equals(currentSheetName)) {
                saveModule();
            } else if (ImportExportHelper.SHEET_RESOURCE.equals(currentSheetName)) {
                saveResource();
            }

        }

        /**
         * 保存语言
         */
        private void saveLanguage() {
            LanguageData language = importContext.getLanguage();
            if (language == null) {
                // 语言页空白，直接中断
                importContext.addError(String.format("%d-%d", currentSheetNo, 0), I18nUtils.getMessage("i18n.import.language_has_more_error"));
                importContext.setShoutDown(true);
                return;
            } else {
                // 保存语言，失败直接中断
                Map<String, String> rs = i18nManagerService.saveLanguage(language.toAddLanguageDto(), false);
                if (MapUtils.isNotEmpty(rs)) {
                    // 保存语言失败，直接中断
                    importContext.addAllError(rs);
                    importContext.setShoutDown(true);
                }
            }
            importContext.getConsumer().accept(new RunningStatus()
                    .setTask(I18nUtils.getMessage("i18n.create.task.name.language"))
                    .setFinished(false)
                    .setProgress(25.0));
        }

        /**
         * 保存模块
         */
        private void saveModule() {
            Map<String, ModuleData> moduleMap = importContext.getModuleMap();
            if (MapUtils.isNotEmpty( moduleMap)) {
                Map<String, String> rs = i18nManagerService.saveModule(moduleMap.values().stream().map(ModuleData::toAddModuleDto).collect(Collectors.toList()), false);
                if (MapUtils.isNotEmpty(rs)) {
                    importContext.addAllError(rs);

                    // 有模块保存失败了
                    for (Map.Entry<String, ModuleData> e : moduleMap.entrySet()) {
                        ModuleData moduleData = e.getValue();
                        if (rs.containsKey(moduleData.getFlagNo())) {
                            moduleData.setCheckSuccess(false);
                        }
                    }
                }
            }
            importContext.getConsumer().accept(new RunningStatus()
                    .setTask(I18nUtils.getMessage("i18n.create.task.name.module"))
                    .setFinished(false)
                    .setProgress(50.0));
        }

        /**
         * 保存资源
         */
        private void saveResource() {
            String languageCode = importContext.getLanguage().getCode();
            Map<String, ModuleData> moduleMap = importContext.getModuleMap();
            Collection<ResourceData> resources = importContext.getResourceMap().values();

            Map<String, List<ResourceData>> moduleResMaps = resources.stream().collect(Collectors.groupingBy(ResourceData::getModuleCode));
            for (Map.Entry<String, List<ResourceData>> e : moduleResMaps.entrySet()) {

                Map<String, String> rs = i18nManagerService.saveResource(e.getKey(), languageCode, e.getValue().stream().map(ResourceData::toResourceDto).collect(Collectors.toList()), true, false);
                if (MapUtils.isNotEmpty(rs)) {
                    importContext.addAllError(rs);
                }
                importContext.addHandleModuleCode(e.getKey());
            }

            for (Map.Entry<String, ModuleData> e : moduleMap.entrySet()) {
                ModuleData moduleData = e.getValue();
                if (!moduleData.isCheckSuccess()) {
                    continue;
                }
                if (!moduleResMaps.containsKey(moduleData.getModuleCode())) {
                    Map<String, String> rs = i18nManagerService.saveResource(e.getKey(), languageCode, new ArrayList<>(), true, false);
                    if (MapUtils.isNotEmpty(rs)) {
                        importContext.addAllError(rs);
                    }
                    importContext.addHandleModuleCode(e.getKey());
                }
            }
            importContext.getConsumer().accept(new RunningStatus()
                    .setTask(I18nUtils.getMessage("i18n.create.task.name.resource"))
                    .setFinished(false)
                    .setProgress(75.0));
        }
    }


}
