package com.supos.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HandleTodoDto {

    @Schema(description = "待办ID")
    @NotNull(message = "待办ID不可为空")
    private Long id;

    @Schema(description = "处理人用户名")
    @NotBlank(message = "处理人用户名不可为空")
    private String username;

}
