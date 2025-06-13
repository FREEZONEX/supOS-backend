package com.supos.common.dto;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TodoQueryDto extends PaginationDTO{

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

    /**
     * 是否我的已办
     */
    @Schema(description = "是否我的已办")
    private Boolean myTodo;

    @Schema(description = "事项信息")
    private String todoMsg;

    /**
     * 处理人用户名
     */
    @Hidden
    private String handlerUsername;

}
