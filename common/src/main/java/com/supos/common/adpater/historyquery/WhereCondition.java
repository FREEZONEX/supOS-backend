package com.supos.common.adpater.historyquery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class WhereCondition {
    /**
     * 时间戳字段使用 {@link com.supos.common.Constants#SYS_FIELD_CREATE_TIME }
     * 其他字段，需要格式形如： 表名(文件别名).字段名
     */
    String name;
    QueryOperator op;
    String value;
    @JsonIgnore
    transient Instant time;

    public void setTime(Instant time) {
        this.time = time;
        if (time != null) {
            value = time.toString();
        }
    }

    public String toString() {
        return String.format("%s %s '%s'", name, op.op, value);
    }
}
