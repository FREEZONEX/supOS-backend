package com.supos.common.dto;

import com.supos.common.Constants;
import lombok.Data;

@Data
public class SimpleUnsInstance {
    Long id;
    String path;
    String alias;
    String tableName;
    Integer dataType;
    Long parentId;
    boolean removeTableWhenDeleteInstance;

    public String getTopic() {
        return Constants.useAliasAsTopic ? alias : path;
    }

    public SimpleUnsInstance(Long id, String path, String alias, String tableName, Integer dataType, Long parentId, boolean removeTableWhenDeleteInstance) {
        this.id = id;
        this.path = path;
        this.alias = alias;
        this.tableName = tableName;
        this.dataType = dataType;
        this.parentId = parentId;
        this.removeTableWhenDeleteInstance = removeTableWhenDeleteInstance;
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
