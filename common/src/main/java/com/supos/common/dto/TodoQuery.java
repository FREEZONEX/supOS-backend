package com.supos.common.dto;

import lombok.Data;

@Data
public class TodoQuery extends PaginationDTO{

    /**
     * 模块编码
     */
    private String moduleCode;

    /**
     * 代办状态：0-未处理 1-已处理
     */
    private Integer status;

    private String todoMsg;

    private String username;

    private String startTime;

    private String endTime;

    /**
     * 是否我的已办
     */
    private Boolean myTodo;

    private String handlerUsername;
}
