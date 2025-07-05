package com.supos.uns.service.exportimport.core;

import com.supos.adpter.eventflow.dao.po.NodeFlowModelPO;
import com.supos.adpter.eventflow.dao.po.NodeFlowPO;
import com.supos.uns.dao.po.DashboardPo;
import lombok.Data;

import java.util.List;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月20日 13:22
 */
@Data
public class EventFlowExportContext {
    private List<NodeFlowPO> flows;
    private List<NodeFlowModelPO> flowModels;
}
