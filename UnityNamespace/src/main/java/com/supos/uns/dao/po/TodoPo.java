package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(TodoPo.TABLE_NAME)
public class TodoPo {

    public static final String TABLE_NAME = "supos_todo";

    @TableId(type = IdType.AUTO)
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

    /**
     * 模块名称
     */
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
    private Long businessId;

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

    /**
     * 处理时间
     */
    private Date handlerTime;

    /**
     * 创建时间
     */
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
