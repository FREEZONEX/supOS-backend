package com.supos.uns.vo;

import lombok.Data;

import java.util.Date;

@Data
public class TodoVo {


    private Long id;

    /**
     * 接收人用户ID
     */
    private String userId;

    /**
     * 接收人用户名
     */
    private String username;

    /**
     * 模块编码
     * @see com.supos.common.enums.SysModuleEnum
     */
    private String moduleCode;

    private String moduleName;

    /**
     * 代办状态：0-未处理 1-已处理
     */
    private Integer status;

    /**
     * 事项信息
     */
    private String todoMsg;

    /**
     * 业务主键
     */
    private String businessId;

    /**
     * 链接
     */
    private String link;

    /**
     * 处理人用户ID
     */
    private String handlerUserId;

    /**
     * 处理人用户名
     */
    private String handlerUsername;

    private Date handlerTime;

    private Date createAt;

    /**
     * 流程ID
     */
    private Long processId;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

}
