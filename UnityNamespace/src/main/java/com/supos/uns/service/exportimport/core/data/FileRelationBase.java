package com.supos.uns.service.exportimport.core.data;

import com.alibaba.excel.annotation.ExcelProperty;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileRelation
 * @date 2025/5/8 17:26
 */
@Data
public class FileRelationBase implements ExportImportData {
    @ExcelProperty(index = 0)
    private String path;
    @ExcelProperty(index = 1)
    private String alias;
    @ExcelProperty(index = 2)
    private String displayName;
    @ExcelProperty(index = 3)
    private String templateAlias;
    @ExcelProperty(index = 4)
    private String fields;
    @ExcelProperty(index = 5)
    private String description;
    @ExcelProperty(index = 6)
    private String autoDashboard;
    @ExcelProperty(index = 7)
    private String persistence;
    @ExcelProperty(index = 8)
    private String label;
}
