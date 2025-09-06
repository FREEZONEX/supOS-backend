package com.supos.uns.service.exportimport.core.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: LabelData
 * @date 2025/5/8 17:09
 */
@Data
public class LabelData implements ExportImportData {

    @ExcelProperty(index = 0)
    private String name;

    @ExcelIgnore
    private String error;
}
