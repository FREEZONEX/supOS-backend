package com.supos.uns.service.exportimport.core.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FolderData
 * @date 2025/5/8 17:14
 */
@Data
public class FolderData implements ExportImportData {

    @ExcelProperty(index = 0)
    private String namespace;
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

    @ExcelIgnore
    private String type;
    @ExcelIgnore
    private List<ExportImportData> children;

    @ExcelIgnore
    private String error;

    public void addChild(ExportImportData child) {
        if (children == null) {
            children = new LinkedList<>();
        }
        children.add(child);
    }
}
