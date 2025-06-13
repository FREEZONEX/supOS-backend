package com.supos.uns.service.exportimport.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.dto.FieldDefine;
import com.supos.common.utils.JsonUtil;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/2/20 19:11
 */
@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExcelTemplateDto {

    /**
     * 批次号
     */
    int batch;
    /**
     * 批次内序号
     */
    int index = -1;
    @NotEmpty
    @Size(max = 63)
    String name;//主题

    String alias;//别名

    String fields;// 字段定义

    String description; //描述


    public void setName(String name) {
        if (name != null && !name.isEmpty()) {
            name = name.trim();
        }
        this.name = name;
    }

    public String gainBatchIndex() {
        return String.format("%d-%d", batch, index);
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
