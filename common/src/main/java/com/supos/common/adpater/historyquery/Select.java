package com.supos.common.adpater.historyquery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

public class Select {
    @Getter
    @Setter
    SelectFunction function;// 函数为空时 表示查询原始数据
    @NotEmpty
    @Getter
    @Setter
    String table;//表名

    @NotEmpty
    @Getter
    @Setter
    String column;//字段名

    @JsonIgnore
    private transient String toStr;

    public String selectName() {
        if (function != null) {
            if (toStr != null) {
                return toStr;
            }
            String s = function.name() + '(' + column + ')';
            return toStr = s;
        } else {
            return column;
        }
    }

}
