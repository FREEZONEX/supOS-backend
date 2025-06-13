package com.supos.uns.service.exportimport.json.data;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.supos.uns.service.exportimport.core.data.FileCalculateBase;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileCalculate
 * @date 2025/5/8 17:27
 */
@Data
public class FileCalculate extends FileCalculateBase {

    @ExcelIgnore
    private String error;

}
