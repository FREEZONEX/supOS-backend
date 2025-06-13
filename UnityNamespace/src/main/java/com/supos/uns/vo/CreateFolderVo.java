package com.supos.uns.vo;

import com.supos.common.annotation.AliasValidator;
import com.supos.common.vo.FieldDefineVo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateFolderVo {

    @Schema(description = "名称")
    @NotEmpty String name;

    @Schema(description = "父节点id")
    Long parentId;

    @Schema(description = "父节点别名")
    String parentAlias;

    @Schema(description = "字段定义")
    @Valid
    FieldDefineVo[] fields;// 模型字段定义，创建实例时可能指定实例的字段 Index

    @Schema(description = "文件夹描述")
    String modelDescription;// 文件夹描述

    /**
     * 文件夹、文件别名
     */
    @Schema(description = "别名")
    @AliasValidator
    String alias;

    @Schema(description = "模板id")
    Long modelId;// 模板id
}
