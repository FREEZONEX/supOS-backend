package com.supos.uns.service.exportimport.core.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileTimeseries
 * @date 2025/5/8 17:19
 */
@Data
public class FileData implements ExportImportData {

    @ExcelProperty(index = 0)
    private String namespace;
    @ExcelProperty(index = 1)
    private String alias;
    @ExcelProperty(index = 3)
    private String displayName;
    @ExcelProperty(index = 4)
    private String templateAlias;
    @ExcelProperty(index = 5)
    private String fields;
    @ExcelProperty(index = 6)
    private String dataType;
    @ExcelProperty(index = 8)
    private String refers;
    @ExcelProperty(index = 9)
    private String expression;
    @ExcelProperty(index = 10)
    private String description;
    @ExcelProperty(index = 11)
    private String label;

    @ExcelProperty(index = 12)
    private String frequency;
    @ExcelProperty(index = 13)
    private String generateDashboard;
    @ExcelProperty(index = 13)
    private String enableHistory;
    @ExcelProperty(index = 13)
    private String mockData;

    @ExcelIgnore
    private String type;
    @ExcelIgnore
    private String error;
}
