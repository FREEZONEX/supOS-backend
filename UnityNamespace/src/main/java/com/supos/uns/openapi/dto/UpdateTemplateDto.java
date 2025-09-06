package com.supos.uns.openapi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.supos.common.annotation.AliasValidator;
import com.supos.common.dto.FieldDefine;
import com.supos.common.utils.PathUtil;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTemplateDto {

    /**
     * 模板名称
     */
    @NotEmpty(message = "模板名称不可为空")
    @Size(max = 63 , message = "uns.template.name.length")
    @Schema(description = "模板名称")
    String name;
    /**
     * 字段定义
     */
    @NotEmpty(message = "字段定义不可为空")
    @Schema(description = "字段定义", name = "definition")
    @JsonAlias({"fields","definition"})
    FieldDefine[] fields;
    /**
     * 模板描述
     */
    @Schema(description = "模板描述")
    @Size(max = 255 , message = "uns.description.length.limit.exceed")
    String description;
}
