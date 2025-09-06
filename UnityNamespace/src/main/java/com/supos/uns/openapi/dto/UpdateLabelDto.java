package com.supos.uns.openapi.dto;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLabelDto {


    @Schema(description = "标签ID")
    @NotNull(message = "标签ID不可为空")
    @Hidden
    private Long id;

    @Schema(description = "标签名称")
    @NotEmpty(message = "标签名称不可为空")
    private String labelName;
}
