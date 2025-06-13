package com.supos.uns.service.exportimport.json.data;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.supos.uns.service.exportimport.core.data.FileReferenceBase;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileReference
 * @date 2025/5/8 16:20
 */
@Data
public class FileReference extends FileReferenceBase {

    @ExcelIgnore
    private String error;

}
