package com.supos.adpter.nodered.service;

import com.alibaba.fastjson.JSONArray;
import com.supos.adpter.nodered.dao.po.NodeFlowModelPO;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import com.supos.adpter.nodered.dto.ExportNodeFlowDto;
import com.supos.adpter.nodered.dto.NodeFlowDto;
import com.supos.adpter.nodered.dto.NodeFlowModelDto;
import com.supos.adpter.nodered.enums.FlowType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnBean(SourceflowAdapterService.class)
public class SourceFlowApiService extends AbstractFlowApiService {

    public SourceFlowApiService(SourceflowAdapterService nodeRedAdapterService) {
        super(nodeRedAdapterService, FlowType.NODERED.getFlowName());
    }

    /**
     * 导出source-flow流程配置
     * @param fids 流程记录id列表
     * @return 流程配置和数据库记录
     */
    public ExportNodeFlowDto exportFlow(Collection<Long> fids) {
        // 根据pid查找flowId
        List<NodeFlowPO> flowRecodes = nodeFlowMapper.queryByIds(fids);
        JSONArray flowNodes = new JSONArray(), globalNodes = new JSONArray();
        // 筛选出需要获取的流程配置
        fillNodes(flowNodes, globalNodes, flowRecodes);
        List<NodeFlowDto> flowDtos = po2Dto(flowRecodes);
        List<NodeFlowModelPO> flowModelPOS = nodeFlowModelMapper.selectByParentIds(fids);
        List<NodeFlowModelDto> flowModelDtos = po3Dto(flowModelPOS);
        List<String> nodeIds = filterSupModelNodeId(flowNodes);
        JSONArray tagArray = null;
        if (!nodeIds.isEmpty()) {
            tagArray = nodeRedAdapterService.retrieveRefTags(nodeIds);
        }
        return new ExportNodeFlowDto(flowDtos, flowModelDtos, tagArray, flowNodes, globalNodes);
    }

    /**
     * 导出所有流程
     * @return 流程配置和数据库记录
     */
    public ExportNodeFlowDto exportAllFlow() {
        List<NodeFlowPO> flowRecodes = nodeFlowMapper.selectAll(FlowType.NODERED.getFlowName());
        List<NodeFlowModelPO> flowModelPOS = nodeFlowModelMapper.selectAll();

        JSONArray flowNodes = new JSONArray(), globalNodes = new JSONArray();
        // 筛选出需要获取的流程配置
        fillNodes(flowNodes, globalNodes);

        List<NodeFlowDto> flowDtos = po2Dto(flowRecodes);
        List<NodeFlowModelDto> flowModelDtos = po3Dto(flowModelPOS);

        List<String> nodeIds = filterSupModelNodeId(flowNodes);
        // 导出流程引用的文件属性
        JSONArray tagArray = nodeRedAdapterService.retrieveRefTags(nodeIds);

        return new ExportNodeFlowDto(flowDtos, flowModelDtos, tagArray, flowNodes, globalNodes);
    }

    private void fillNodes(JSONArray flowNodes, JSONArray globalNodes, List<NodeFlowPO> flowRecodes) {
        // 获取所有的流程配置
        JSONArray allFlowConfigs = nodeRedAdapterService.retrieveAllFlowConfig();
        if (allFlowConfigs == null) {
            return;
        }
        Set<String> flowIds = flowRecodes.stream().map(NodeFlowPO::getFlowId).collect(Collectors.toSet());
        for (int i = 0; i < allFlowConfigs.size(); i++) {
            String z = allFlowConfigs.getJSONObject(i).getString("z");
            String id = allFlowConfigs.getJSONObject(i).getString("id");
            String type = allFlowConfigs.getJSONObject(i).getString("type");
            // 筛选出全局节点
            if (StringUtils.isEmpty(z) && !"tab".equals(type)) {
                globalNodes.add(allFlowConfigs.getJSONObject(i));
            }
            if (flowIds.contains(z) || flowIds.contains(id)) {
                flowNodes.add(allFlowConfigs.getJSONObject(i));
            }
        }
    }

}
