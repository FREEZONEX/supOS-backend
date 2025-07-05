package com.supos.uns.service.exportimport.core;

import com.supos.adpter.nodered.dao.po.NodeFlowModelPO;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import lombok.Data;

import java.util.List;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月20日 13:22
 */
@Data
public class SourceFlowExportContext {
    private List<NodeFlowPO> flows;
    private List<NodeFlowModelPO> flowModels;
}
