package com.supos.uns.service.exportimport.core.data;

import com.alibaba.excel.annotation.ExcelProperty;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FolderData
 * @date 2025/5/8 17:14
 */
@Data
public class FolderDataBase implements ExportImportData {
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
}
