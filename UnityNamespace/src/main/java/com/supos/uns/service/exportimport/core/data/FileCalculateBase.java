package com.supos.uns.service.exportimport.core.data;

import com.alibaba.excel.annotation.ExcelProperty;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileCalculate
 * @date 2025/5/8 17:27
 */
@Data
public class FileCalculateBase implements ExportImportData {

    @ExcelProperty(index = 0)
    private String path;
    @ExcelProperty(index = 1)
    private String alias;
    @ExcelProperty(index = 2)
    private String displayName;
    @ExcelProperty(index = 3)
    private String fields;
    @ExcelProperty(index = 4)
    private String refers;
    @ExcelProperty(index = 5)
    private String expression;
    @ExcelProperty(index = 6)
    private String description;
    @ExcelProperty(index = 7)
    private String autoDashboard;
    @ExcelProperty(index = 8)
    private String persistence;
    @ExcelProperty(index = 9)
    private String label;

    @Override
    public void handleRefers(String refers) {
        this.refers = refers;
    }
}
