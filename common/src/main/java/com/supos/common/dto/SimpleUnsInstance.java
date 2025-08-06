package com.supos.common.dto;

import com.supos.common.Constants;
import com.supos.common.utils.JsonUtil;
import lombok.Data;

import java.util.Set;

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
    boolean removeDashboard;
    boolean removeTableWhenDeleteInstance;
    Set<Long> labelIds;
    Integer flags;

    public String getTopic() {
        return Constants.useAliasAsTopic ? alias : path;
    }

    public SimpleUnsInstance() {

    }

    public SimpleUnsInstance(Long id, String path, String alias, String tableName, Integer dataType, Long parentId,
                             boolean removeTableWhenDeleteInstance,
                             boolean removeDashboard,
                             FieldDefine[] fields, String name) {
        this.id = id;
        this.path = path;
        this.alias = alias;
        this.tableName = tableName;
        this.dataType = dataType;
        this.parentId = parentId;
        this.fields = fields;
        this.removeDashboard = removeDashboard;
        this.removeTableWhenDeleteInstance = removeTableWhenDeleteInstance;
        this.name = name;
    }

    public String getTableNameOnly() {
        return tableName;
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

    public String toString() {
        return JsonUtil.jackToJson(this);
    }
}
