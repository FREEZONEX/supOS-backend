package com.supos.uns.service.exportimport.core.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FileJsonbData implements ExportImportData {

    @ExcelProperty(index = 0)
    private String namespace;
    @ExcelProperty(index = 1)
    private String alias;
    @ExcelProperty(index = 2)
    private String displayName;
    @ExcelProperty(index = 3)
    private String description;
    @ExcelProperty(index = 4)
    private String enableHistory;
    @ExcelProperty(index = 5)
    private String label;
    @ExcelProperty(index = 6)
    private String generateDashboard;
    @ExcelProperty(index = 7)
    @JsonProperty("topicType")
    private String parentDataType;

    @ExcelIgnore
    private String name;
}
