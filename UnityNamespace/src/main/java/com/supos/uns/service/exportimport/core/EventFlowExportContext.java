package com.supos.uns.service.exportimport.core;

import com.supos.adpter.nodered.dao.po.NodeFlowModelPO;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import com.supos.adpter.nodered.dto.ExportNodeFlowDto;
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

    private ExportNodeFlowDto reqDto;
}
