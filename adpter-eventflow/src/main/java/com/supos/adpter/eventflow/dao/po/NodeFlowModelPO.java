package com.supos.adpter.eventflow.dao.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeFlowModelPO {

    /**
     * 流程ID
     */
    private long parentId;

    /**
     * 关联模型 topic
     */
    private String topic;
}
