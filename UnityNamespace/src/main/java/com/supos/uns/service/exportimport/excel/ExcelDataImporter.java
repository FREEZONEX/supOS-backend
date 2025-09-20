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
import com.supos.common.utils.I18nUtils;
import com.supos.common.RunningStatus;
import com.supos.uns.service.UnsAddService;
import com.supos.uns.service.UnsLabelService;
import com.supos.uns.service.UnsManagerService;
import com.supos.uns.service.UnsTemplateService;
import com.supos.uns.service.exportimport.core.DataImporter;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        // 读取excel进行处理
        log.info(file.getAbsolutePath());
        ExcelUtil.readBySax(file, -1, new MExcelRowHandler(getContext()));
    }

    @Override
    public void writeError(File srcfile, File outFile) {
        try {
            String templatePath = Constants.EXCEL_TEMPLATE_PATH;
            ExcelWriter excelWriter = EasyExcel.write(outFile).withTemplate(new ClassPathResource(templatePath).getInputStream()).build();

            com.alibaba.excel.ExcelReader excelReader =EasyExcel.read(srcfile).ignoreEmptyRow(false).build();

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

    /**
     * 保存导入的数据
     * @param excelTypeEnum
     */
    public void doImport(ExcelTypeEnum excelTypeEnum) {
        if (excelTypeEnum == ExcelTypeEnum.Template) {
            importTemplate(getContext());
            getContext().getConsumer().accept(new RunningStatus()
                    .setTask(I18nUtils.getMessage("uns.create.task.name.template"))
                    .setFinished(false)
                    .setProgress(20.0));
        } else if (excelTypeEnum == ExcelTypeEnum.Label) {
            importLabel(getContext());
            getContext().getConsumer().accept(new RunningStatus()
                    .setTask(I18nUtils.getMessage("uns.create.task.name.label"))
                    .setFinished(false)
                    .setProgress(40.0));
        } else if (excelTypeEnum == ExcelTypeEnum.Folder) {
            importFolder(getContext());
            getContext().getConsumer().accept(new RunningStatus()
                    .setTask(I18nUtils.getMessage("uns.create.task.name.folder"))
                    .setFinished(false)
                    .setProgress(60.0));
        } else if (excelTypeEnum == ExcelTypeEnum.FILE_TIMESERIES) {
            // 保存时序文件
            importFile(getContext(), Constants.TIME_SEQUENCE_TYPE);
        } else if (excelTypeEnum == ExcelTypeEnum.FILE_RELATION) {
            // 保存关系文件
            importFile(getContext(), Constants.RELATION_TYPE);
        } else if (excelTypeEnum == ExcelTypeEnum.FILE_CALCULATE) {
            // 保存计算文件
            separationRefer(getContext().getFileCalculateMap());
            importFile(getContext(), ExcelImportContext.REFER_DATATYPE);
            importFile(getContext(), Constants.CALCULATION_REAL_TYPE);
        } else if (excelTypeEnum == ExcelTypeEnum.FILE_AGGREGATION) {
            // 保存聚合文件
            separationRefer(getContext().getFileAggregationMap());
            importFile(getContext(), ExcelImportContext.REFER_DATATYPE);
            importFile(getContext(), Constants.MERGE_TYPE);
        } else if (excelTypeEnum == ExcelTypeEnum.FILE_REFERENCE) {
            // 保存引用文件
            separationRefer(getContext().getFileReferenceMap());
            importFile(getContext(), ExcelImportContext.REFER_DATATYPE);
            importFile(getContext(), Constants.CITING_TYPE);
            getContext().getConsumer().accept(new RunningStatus()
                    .setTask(I18nUtils.getMessage("uns.create.task.name.file"))
                    .setFinished(false)
                    .setProgress(80.0));
        }
    }

    /**
     * 自定义excel处理器
     */
    class MExcelRowHandler implements RowHandler {
        private long skipRow = 4;

        private ExcelTypeEnum currentExcelType;
        private Map<Integer, String> headerIndex = new HashMap<>();
        private long batch = 2000;

        private ExcelImportContext context;

        public MExcelRowHandler(ExcelImportContext context) {
            this.context = context;
        }

        /**
         * 处理某一行数据
         * @param sheetIndex 当前Sheet序号
         * @param l   当前行号，从0开始计数
         * @param list   行数据，每个Object表示一个单元格的值
         */
        @Override
        public void handle(int sheetIndex, long l, List<Object> list) {
            // 解析出当前的sheet页，和表头信息
            if (currentExcelType == null && l == 0L) {
                currentExcelType = ExcelTypeEnum.valueOfIndex(sheetIndex);
                for (int i = 0; i < list.size(); i++) {
                    headerIndex.put(i, list.get(i).toString());
                }
            }

            // 跳过demo行
            if (l < skipRow) {
                return;
            }

            // 抽取当前行的数据放入map中
            Map<String, Object> dataMap = new HashMap<>(list.size());
            for (int i = 0, sz = list.size(); i < sz; i++) {
                dataMap.put(headerIndex.get(i), list.get(i));
            }

            if (currentExcelType != ExcelTypeEnum.Explanation && currentExcelType != ExcelTypeEnum.ERROR) {
                getParser(currentExcelType).parseExcel(String.format("%d-%d", sheetIndex, (int) l), dataMap, context);

                if (currentExcelType == ExcelTypeEnum.Template
                        || currentExcelType == ExcelTypeEnum.Label
                        || currentExcelType == ExcelTypeEnum.FILE_TIMESERIES
                        || currentExcelType == ExcelTypeEnum.FILE_RELATION) {
                    // 流式数据前后无依赖，可进行分批保存
                    if (l % batch == 0) {
                        doImport(currentExcelType);
                    }
                } else if (currentExcelType == ExcelTypeEnum.Folder
                        || currentExcelType == ExcelTypeEnum.FILE_CALCULATE
                        || currentExcelType == ExcelTypeEnum.FILE_AGGREGATION
                        || currentExcelType == ExcelTypeEnum.FILE_REFERENCE) {
                    // 这几类文件，需要等待所有数据处理完成后再进行保存
                }
            }
        }

        /**
         * 每个sheet处理完后的处理逻辑
         */
        @Override
        public void doAfterAllAnalysed() {
            doImport(currentExcelType);

            if (currentExcelType == ExcelTypeEnum.FILE_REFERENCE) {
                log.info("import running time:{}s", getStopWatch().getTotalTimeSeconds());
                log.info(getStopWatch().prettyPrint());
            }

            currentExcelType = null;
            headerIndex.clear();

        }
    }
}
