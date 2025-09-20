package com.supos.i18n.service.excel;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.supos.i18n.service.I18nExcelService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;

import java.util.List;
import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelErrorWriteHandler
 * @date 2025/5/8 20:52
 */
public class ExcelWriteHandler implements CellWriteHandler {

    private CellStyle errorCellStyle;
    private CellStyle headCellStyle;

    private Map<Integer, Map<Integer, String>> dataError;

    public ExcelWriteHandler() {
    }

    public ExcelWriteHandler(Map<Integer, Map<Integer, String>> dataError) {
        this.dataError = dataError;
    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        // 说明页不用特殊处理
        String sheetName = writeSheetHolder.getSheetName();
        if (sheetName.equals(ImportExportHelper.SHEET_EXPLANATION)) {
            return;
        }

        if (cell.getRowIndex() == 0) {
            if (MapUtils.isNotEmpty(dataError)) {
                if (hasError(writeSheetHolder.getSheetNo(), cell.getRowIndex())) {
                    Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
                    if (this.errorCellStyle == null) {
                        CellStyle cellStyle = workbook.createCellStyle();
                        cellStyle.setFillForegroundColor(IndexedColors.RED1.getIndex());
                        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        this.errorCellStyle = cellStyle;
                    }

                    cell.setCellStyle(this.errorCellStyle);
                    return;
                }
            }
            // 表头
            Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
            if (this.headCellStyle == null) {
                CellStyle cellStyle = workbook.createCellStyle();
                cellStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                this.headCellStyle = cellStyle;
            }

            cell.setCellStyle(this.headCellStyle);
        } else {
            // 数据行
            if (MapUtils.isEmpty(dataError)) {
                return;
            }
            if (hasError(writeSheetHolder.getSheetNo(), cell.getRowIndex())) {
                Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
                if (this.errorCellStyle == null) {
                    CellStyle cellStyle = workbook.createCellStyle();
                    cellStyle.setFillForegroundColor(IndexedColors.RED1.getIndex());
                    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    this.errorCellStyle = cellStyle;
                }

                cell.setCellStyle(this.errorCellStyle);
            }
        }
    }

    private boolean hasError(int sheetNo, int rowIndex) {
        Map<Integer, String> subErrorMap = dataError.get(sheetNo);
        if (subErrorMap != null) {
            String error = subErrorMap.get(rowIndex);
            if (StringUtils.isNotBlank(error)) {
                return true;
            }
        }
        return false;
    }
}
