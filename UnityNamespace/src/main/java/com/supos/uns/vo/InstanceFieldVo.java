package com.supos.uns.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.Constants;
import com.supos.common.annotation.ReferUnsValidator;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ReferUnsValidator
public class InstanceFieldVo {
    String id;// uns id, 只存储id
    String alias;//  前端传 id, 后端填充 alias
    String path; //  前端传 id, 后端填充 path
    String field;
    Boolean uts;

    public String getTopic() {
        return Constants.useAliasAsTopic ? alias : path;
    }

    public InstanceFieldVo() {
    }

//    public InstanceFieldVo(String id, String field) {
//        this.id = id;
//        this.field = field;
//    }
//
//    public InstanceFieldVo(String alias, String field) {
//        this.alias = alias;
//        this.field = field;
//    }
}