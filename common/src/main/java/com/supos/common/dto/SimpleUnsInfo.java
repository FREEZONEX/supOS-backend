package com.supos.common.dto;

public interface SimpleUnsInfo {
    Long getId();
    String getAlias();
    String getName();
    String getTableName();
    String getPath();
    Integer getDataType();
    FieldDefine[] getFields();
}
