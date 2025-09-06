package com.supos.uns.vo;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class TodoVo {


    @Schema(description = "ID")
    private Long id;

    /**
     * 接收人用户ID
     */
    @Schema(description = "接收人用户ID")
    private String userId;

    /**
     * 接收人用户名
     */
    @Schema(description = "接收人用户ID")
    private String username;

    /**
     * 模块编码
     * @see com.supos.common.enums.SysModuleEnum
     */
    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "接收人用户ID")
    private String moduleName;

    /**
     * 代办状态：0-未处理 1-已处理
     */
    @Schema(description = "代办状态：0-未处理 1-已处理")
    private Integer status;

    /**
     * 事项信息
     */
    @Schema(description = "事项信息")
    private String todoMsg;

    /**
     * 业务主键
     */
    @Hidden
    private String businessId;

    /**
     * 链接
     */
    @Hidden
    private String link;

    /**
     * 处理人用户ID
     */
    @Schema(description = "处理人用户ID")
    private String handlerUserId;

    /**
     * 处理人用户名
     */
    @Schema(description = "处理人用户名")
    private String handlerUsername;

    @Schema(description = "处理时间")
    private Date handlerTime;

    @Schema(description = "创建时间")
    private Date createAt;

    /**
     * 流程ID
     */
    @Hidden
    private Long processId;

    /**
     * 流程实例ID
     */
    @Hidden
    private String processInstanceId;

}
