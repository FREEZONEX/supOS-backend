package com.supos.common.dto;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TodoOpenQueryDto extends PaginationDTO{

    /**
     * 模块编码
     */
    @Schema(description = "模块编码")
    private String moduleCode;

    /**
     * 代办状态：0-未处理 1-已处理
     */
    @Schema(description = "代办状态：0-未处理 1-已处理")
    private Integer status;

    @Schema(description = "任务")
    private String todoMsg;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "开始时间，UTC格式：如2014-11-11T12:00:00Z")
    private String startTime;

    @Schema(description = "结束时间，UTC格式：如2014-11-11T12:00:00Z")
    private String endTime;

    @Schema(description = "处理人用户名")
    private String handlerUsername;
}
