package com.supos.uns.service.exportimport.json.data;

import com.supos.adpter.nodered.dao.po.NodeFlowModelPO;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import lombok.Data;

import java.util.List;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月20日 16:15
 */
@Data
public class SourceFlowJsonWrapper {
    private List<NodeFlowPO> flows;
    private List<NodeFlowModelPO> flowModels;
}
