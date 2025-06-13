package com.supos.uns.vo;

import com.supos.camunda.po.ProcessDefinitionPo;
import com.supos.common.dto.AlarmRuleDefine;
import com.supos.uns.dao.po.AlarmHandlerPo;
import lombok.Data;

import java.util.List;

@Data
public class AlarmRuleSearchResult {

    String id;

    /**
     * namespace : data_path
     */
    String name;

    /**
     * 描述
     */
    String description;

    /**
     * 报警规则
     */
    String topic;

    /**
     * 引用的实例
     */
    String refUns;

    /**
     * 字段JSON
     */
    String field;

    /**
     * 报警数量
     */
    long alarmCount;
    /**
     * 未读数
     */
    long noReadCount;

    /**
     * 规则定义
     */
    AlarmRuleDefine alarmRuleDefine;

    Integer withFlags;//接收方式 16-人员 32-工作流程

    /**
     * 处理人列表
     */
    List<AlarmHandlerPo> handlerList;

    /**
     * 工作流流程定义
     */
    ProcessDefinitionPo processDefinition;
}
