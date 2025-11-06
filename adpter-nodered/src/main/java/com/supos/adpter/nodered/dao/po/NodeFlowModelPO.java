package com.supos.adpter.nodered.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("supos_node_flow_models")
public class NodeFlowModelPO {

    /**
     * 流程ID
     */
    private long parentId;

    /**
     * 关联模型 topic
     */
    private String topic;

    /**
     * 模型alias
     */
    private String alias;
}
