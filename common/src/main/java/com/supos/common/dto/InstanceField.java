package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.Constants;
import com.supos.common.annotation.ReferUnsValidator;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ReferUnsValidator
public class InstanceField {
    Long id;// uns id, 只存储id
    String alias;//  前端传 id, 后端填充 alias
    String path; //  前端传 id, 后端填充 path
    String field;
    Boolean uts; // true--计算型实例，使用当前uns的时间戳

    public String getTopic() {
        return Constants.useAliasAsTopic ? alias : path;
    }

    public InstanceField() {
    }

    public InstanceField(Long id, String field) {
        this.id = id;
        this.field = field;
    }

    public InstanceField(String alias, String field) {
        this.alias = alias;
        this.field = field;
    }
}