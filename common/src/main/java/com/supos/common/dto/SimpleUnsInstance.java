package com.supos.common.dto;

import com.supos.common.Constants;
import lombok.Data;

@Data
public class SimpleUnsInstance {
    Long id;
    String name;
    String path;
    String alias;
    String tableName;
    Integer dataType;
    Long parentId;
    FieldDefine[] fields;
    boolean removeTableWhenDeleteInstance;

    public String getTopic() {
        return Constants.useAliasAsTopic ? alias : path;
    }
    public SimpleUnsInstance() {

    }
    public SimpleUnsInstance(Long id, String path, String alias, String tableName, Integer dataType, Long parentId, boolean removeTableWhenDeleteInstance, FieldDefine[] fields, String name) {
        this.id = id;
        this.path = path;
        this.alias = alias;
        this.tableName = tableName;
        this.dataType = dataType;
        this.parentId = parentId;
        this.fields = fields;
        this.removeTableWhenDeleteInstance = removeTableWhenDeleteInstance;
        this.name = name;
    }

    public String getTableName() {
        if (tableName != null) {
            return tableName;
        }
        if (alias != null) {
            return alias;
        }
        return path;
    }
}
