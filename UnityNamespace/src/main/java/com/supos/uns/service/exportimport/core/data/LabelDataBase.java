package com.supos.uns.service.exportimport.core.data;

import com.alibaba.excel.annotation.ExcelProperty;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: LabelData
 * @date 2025/5/8 17:09
 */
@Data
public class LabelDataBase implements ExportImportData {
    @ExcelProperty(index = 0)
    private String name;
}
