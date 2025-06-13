package com.supos.common.adpater.historyquery;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class HistoryQueryResult {
    int code;
    String message;

    List<FieldsAndData> results;

    Collection<String> notExists;// 不存在的表名 或 表名.字段名
}