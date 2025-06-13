package com.supos.uns.service.exportimport.excel;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
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
public class ExcelErrorWriteHandler implements CellWriteHandler {

    private CellStyle cellStyle;

    private ExcelImportContext context;

    public ExcelErrorWriteHandler(ExcelImportContext context) {
        this.context = context;
    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        if (hasError(writeSheetHolder.getSheetNo(), cell.getRowIndex())) {
            Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
            if (this.cellStyle == null) {
                CellStyle cellStyle = workbook.createCellStyle();
                cellStyle.setFillForegroundColor(IndexedColors.RED1.getIndex());
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                this.cellStyle = cellStyle;
            }

            cell.setCellStyle(this.cellStyle);
        }
    }

    private boolean hasError(int sheetNo, int rowIndex) {
        Map<Integer, String> subErrorMap = context.getError().get(sheetNo);
        if (subErrorMap != null) {
            String error = subErrorMap.get(rowIndex);
            if (StringUtils.isNotBlank(error)) {
                return true;
            }
        }
        return false;
    }
}
