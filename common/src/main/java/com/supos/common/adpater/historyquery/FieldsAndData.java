package com.supos.common.adpater.historyquery;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FieldsAndData {
    @JsonProperty("alias")
    String table; // 表名 (uns文件的别名）
    String column;//字段名
    SelectFunction function;// select的函数, 可能为null(查询原始值)
    boolean hasNext;// 是否有下一页
    /**
     * 顺序：{@link com.supos.common.Constants#SYS_FIELD_CREATE_TIME },
     * 被 select的 字段名,
     * {@link com.supos.common.Constants#QOS_FIELD}
     */
    List<String> fields;
    List<Object[]> datas;

}
