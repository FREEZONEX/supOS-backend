package com.supos.uns.service.exportimport.core.data;

import com.alibaba.excel.annotation.ExcelProperty;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileReference
 * @date 2025/5/8 16:20
 */
@Data
public class FileReferenceBase implements ExportImportData {

    @ExcelProperty(index = 0)
    private String path;
    @ExcelProperty(index = 1)
    private String alias;
    @ExcelProperty(index = 2)
    private String displayName;
    @ExcelProperty(index = 3)
    private String refers;
    @ExcelProperty(index = 4)
    private String description;
    @ExcelProperty(index = 5)
    private String autoDashboard;
    @ExcelProperty(index = 6)
    private String persistence;
    @ExcelProperty(index = 7)
    private String label;

    @Override
    public void handleRefers(String refers) {
        this.refers = refers;
    }
}
