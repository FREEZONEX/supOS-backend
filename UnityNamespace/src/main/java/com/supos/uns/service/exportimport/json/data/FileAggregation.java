package com.supos.uns.service.exportimport.json.data;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.supos.uns.service.exportimport.core.data.FileAggregationBase;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileAggregation
 * @date 2025/5/8 17:26
 */
@Data
public class FileAggregation extends FileAggregationBase {

    @ExcelIgnore
    private String error;

}
