package com.supos.camunda.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 流程定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("supos_workflow_process")
public class ProcessDefinitionPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 描述
     */
    private String description;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;

    /**
     * 流程名称
     */
    private String processDefinitionName;

    /**
     * 流程Key
     */
    private String processDefinitionKey;

    /**
     * 0草稿：未部署
     * 1运行：已部署，运行中
     * 2暂停：已部署，已暂停
     */
    private Integer status;

    /**
     * 部署ID
     */
    private String deployId;

    /**
     * 部署名称
     */
    private String deployName;

    /**
     * 部署时间
     */
    private Date deployTime;

    private String bpmnXml;
}
