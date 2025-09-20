package com.supos.i18n.service.excel;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.read.metadata.holder.ReadHolder;
import com.alibaba.excel.read.metadata.holder.ReadSheetHolder;
import com.alibaba.excel.write.metadata.WriteSheet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelRowErrorHandler
 * @date 2025/5/6 14:29
 */
@Slf4j
public class ExcelRowErrorHandler implements ReadListener<Map<Integer, String>> {

    private Map<Integer, Map<Integer, String>> dataError;

    private ExcelWriter excelWriter;

    private WriteSheet writeSheet;

    private List<Map<Integer, String>> dataList = new ArrayList<>(200);

    public ExcelRowErrorHandler(ExcelWriter excelWriter, WriteSheet writeSheet, Map<Integer, Map<Integer, String>> dataError) {
        this.excelWriter = excelWriter;
        this.writeSheet = writeSheet;
        this.dataError = dataError;
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        ReadListener.super.onException(exception, context);
    }

    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
        String sheetName = context.readSheetHolder().getSheetName();
        int rowIndex = context.readRowHolder().getRowIndex();

        Map<Integer, String> data = new HashMap<>();
        for (Map.Entry<Integer, ReadCellData<?>> entry : headMap.entrySet()) {
            data.put(entry.getKey(), entry.getValue().getStringValue());
        }
        if (ImportExportHelper.SHEET_EXPLANATION.equals(sheetName)) {
            dataList.add(data);
        } else {
            dataList.add(addError(sheetName, rowIndex, data));
        }

        writeSheet.setSheetName(sheetName);
    }

    @Override
    public void extra(CellExtra extra, AnalysisContext context) {
        ReadListener.super.extra(extra, context);
    }

    @Override
    public boolean hasNext(AnalysisContext context) {
        return ReadListener.super.hasNext(context);
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        String sheetName =context.readSheetHolder().getSheetName();
        int rowIndex = context.readRowHolder().getRowIndex();

        if (ImportExportHelper.SHEET_EXPLANATION.equals(sheetName)) {
            dataList.add(data);
        } else {
            dataList.add(addError(sheetName, rowIndex, data));
        }

        if (dataList.size() % 500 == 0) {
            write(context);
        }
    }

    private Map<Integer, String> addError(String sheetName, int rowIndex, Map<Integer, String> data) {
        int sheetNo = ImportExportHelper.SHEET_INDEX.get(sheetName);
        String error = null;
        Map<Integer, String> subErrorMap = dataError.get(sheetNo);
        if (subErrorMap != null) {
            error = subErrorMap.get(rowIndex);

        }
        int errorIndex = ImportExportHelper.errorIndex(sheetName);
        if (StringUtils.isNotBlank(error)) {
            // 填充空单元格
            if (data.size() < errorIndex) {
                for (int i = data.size(); i < errorIndex; i++) {
                    data.put(i, "");
                }
            }
            data.put(errorIndex, String.format("Import Error:%s", error));
            //context.setActiveExcelType(excelType);
        } else {
            data.remove(errorIndex);
        }
        return data;
    }

    private void write(AnalysisContext context) {
        excelWriter.write(dataList, writeSheet);
        dataList.clear();
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        excelWriter.write(dataList, writeSheet);
        dataList.clear();
    }
}
