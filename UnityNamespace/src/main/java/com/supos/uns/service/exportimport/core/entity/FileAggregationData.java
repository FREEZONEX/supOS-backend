package com.supos.uns.service.exportimport.core.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileAggregation
 * @date 2025/5/8 17:26
 */
@Data
public class FileAggregationData implements ExportImportData {

    @ExcelProperty(index = 0)
    private String namespace;
    @ExcelProperty(index = 1)
    private String alias;

    @ExcelProperty(index = 2)
    private String displayName;
    @ExcelProperty(index = 3)
    private String refers;

    @ExcelProperty(index = 4)
    private String frequency;
    @ExcelProperty(index = 5)
    private String description;
    @ExcelProperty(index = 6)
    private String generateDashboard;

    @ExcelProperty(index = 7)
    private String enableHistory;
    @ExcelProperty(index = 8)
    private String label;

    @ExcelProperty(index = 9)
    private String parentDataType;

    @ExcelIgnore
    private String error;
}
