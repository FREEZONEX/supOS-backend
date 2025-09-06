package com.supos.uns.service.exportimport.core.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileReference
 * @date 2025/5/8 16:20
 */
@Data
public class FileReferenceData implements ExportImportData {

    @ExcelProperty(index = 0)
    private String namespace;
    @ExcelProperty(index = 1)
    private String alias;

    @ExcelProperty(index = 2)
    private String displayName;

    @ExcelProperty(index = 3)
    private String refers;

    @ExcelProperty(index = 4)
    private String description;
    @ExcelProperty(index = 5)
    private String generateDashboard;

    @ExcelProperty(index = 6)
    private String enableHistory;
    @ExcelProperty(index = 7)
    private String label;

    @ExcelIgnore
    private String error;
}
