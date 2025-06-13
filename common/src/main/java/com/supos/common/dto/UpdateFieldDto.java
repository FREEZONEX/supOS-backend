package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.utils.JsonUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateFieldDto {

    String alias; // 别名即表名

    String topic;

    Collection<FieldDefine> newFields;// 新增的字段定义

    Collection<FieldDefine> delFields;// 删除的字段定义


    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
