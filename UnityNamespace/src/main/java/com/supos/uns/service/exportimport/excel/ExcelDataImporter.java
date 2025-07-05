package com.supos.uns.service.exportimport.excel;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.supos.common.Constants;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.exception.BuzException;
import com.supos.uns.service.UnsAddService;
import com.supos.uns.service.UnsLabelService;
import com.supos.uns.service.UnsManagerService;
import com.supos.uns.service.UnsTemplateService;
import com.supos.uns.service.exportimport.core.DataImporter;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelDataImporter
 * @date 2025/5/10 13:04
 */
@Slf4j
public class ExcelDataImporter extends DataImporter {

    public ExcelDataImporter(ExcelImportContext context, UnsManagerService unsManagerService, UnsLabelService unsLabelService,
                             UnsTemplateService unsTemplateService, UnsAddService unsAddService) {
        super(context, unsManagerService, unsLabelService, unsTemplateService, unsAddService);
    }

    @Override
    public void importData(File file) {
        ExcelUtil.readBySax(file, -1, new MExcelRowHandler(getContext()));
    }

    @Override
    public void writeError(File srcfile, File outFile) {
        try {
            ExcelWriter excelWriter = EasyExcel.write(outFile).withTemplate(new ClassPathResource(Constants.EXCEL_TEMPLATE_PATH).getInputStream()).build();

            com.alibaba.excel.ExcelReader excelReader =EasyExcel.read(srcfile).build();

            ReadSheet[] readSheets = new ReadSheet[ExcelTypeEnum.size()];
            for (ExcelTypeEnum obj : ExcelTypeEnum.values()) {
                if (obj != ExcelTypeEnum.ERROR) {
                    WriteSheet writeSheet = EasyExcel.writerSheet(obj.getIndex()).registerWriteHandler(new ExcelErrorWriteHandler(getContext())).build();
                    ReadSheet readSheet =
                            EasyExcel.readSheet(obj.getIndex()).registerReadListener(new ExcelRowErrorHandler(excelWriter, writeSheet, getContext())).build();
                    readSheets[obj.getIndex()] = readSheet;
                }
            }
            excelReader.read(readSheets);
            excelReader.finish();

            excelWriter.writeContext().getWorkbook().setActiveSheet(getContext().getActiveExcelType().getIndex());

            excelWriter.finish();
        } catch (Throwable e) {
            log.error("导入失败", e);
            throw new BuzException(e.getMessage());
        }
    }

    class MExcelRowHandler implements RowHandler {
        private long skipRow = 4;

        private ExcelTypeEnum currentExcelType;
        private Map<Integer, String> headerIndex = new HashMap<>();
        private long batch = 2000;

        private ExcelImportContext context;

        public MExcelRowHandler(ExcelImportContext context) {
            this.context = context;
        }

        @Override
        public void handle(int sheetIndex, long l, List<Object> list) {
            if (currentExcelType == null && l == 0L) {
                currentExcelType = ExcelTypeEnum.valueOfIndex(sheetIndex);
                for (int i = 0; i < list.size(); i++) {
                    headerIndex.put(i, list.get(i).toString());
                }
            }

            if (l < skipRow) {
                return;
            }

            Map<String, Object> dataMap = new HashMap<>(list.size());
            for (int i = 0, sz = list.size(); i < sz; i++) {
                dataMap.put(headerIndex.get(i), list.get(i));
            }

            if (currentExcelType != ExcelTypeEnum.Explanation && currentExcelType != ExcelTypeEnum.ERROR) {
                getParser(currentExcelType).parseExcel(sheetIndex, (int) l, dataMap, context);
                if (l % batch == 0) {
                    doImport(currentExcelType);
                }
            }
        }

        @Override
        public void doAfterAllAnalysed() {
            if (currentExcelType == ExcelTypeEnum.Template) {
                importTemplate(context);
            } else if (currentExcelType == ExcelTypeEnum.Label) {
                importLabel(context);
            } else if (currentExcelType == ExcelTypeEnum.Folder) {
                importFolder(context);
            } else if (currentExcelType == ExcelTypeEnum.FILE_TIMESERIES
                    || currentExcelType == ExcelTypeEnum.FILE_RELATION
                    || currentExcelType == ExcelTypeEnum.FILE_CALCULATE
                    || currentExcelType == ExcelTypeEnum.FILE_AGGREGATION
                    || currentExcelType == ExcelTypeEnum.FILE_REFERENCE) {
                importFile(context, currentExcelType);
            }

            if (currentExcelType == ExcelTypeEnum.FILE_REFERENCE) {
                log.info("import running time:{}s", getStopWatch().getTotalTimeSeconds());
                log.info(getStopWatch().prettyPrint());
            }

            currentExcelType = null;
            headerIndex.clear();

        }
    }
}
