package com.supos.uns.service.exportimport.core.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.supos.common.dto.FieldDefine;
import com.supos.common.vo.FieldDefineVo;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileTimeseries
 * @date 2025/5/8 17:19
 */
@Data
public class FileData implements ExportImportData {

    @ExcelProperty(index = 0)
    private String namespace;
    @ExcelProperty(index = 1)
    private String alias;
    @ExcelProperty(index = 3)
    private String displayName;
    @ExcelProperty(index = 4)
    private String templateAlias;
    @ExcelProperty(index = 5)
    private FieldDefineVo[] fields;
    @ExcelProperty(index = 6)
    private String dataType;
    @ExcelProperty(index = 8)
    private String refers;
    @ExcelProperty(index = 9)
    private String expression;
    @ExcelProperty(index = 10)
    private String description;
    @ExcelProperty(index = 11)
    private String label;

    @ExcelProperty(index = 12)
    private String frequency;
    @ExcelProperty(index = 13)
    private String generateDashboard;
    @ExcelProperty(index = 13)
    private String enableHistory;
    @ExcelProperty(index = 13)
    private String mockData;
    @ExcelProperty(index = 14)
    @JsonProperty("topicType")
    private String parentDataType;

    @ExcelIgnore
    private String type;
    @ExcelIgnore
    private String error;

    @ExcelIgnore
    private String name;
}
