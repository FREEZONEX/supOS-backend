package com.supos.adpter.nodered.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supos.adpter.nodered.dao.mapper.NodeFlowMapper;
import com.supos.adpter.nodered.dao.mapper.NodeFlowModelMapper;
import com.supos.adpter.nodered.dao.po.NodeFlowModelPO;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import com.supos.adpter.nodered.dto.ExportNodeFlowDto;
import com.supos.adpter.nodered.dto.NodeFlowDto;
import com.supos.adpter.nodered.dto.NodeFlowModelDto;
import com.supos.adpter.nodered.enums.FlowStatus;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractFlowApiService {

    protected NodeRedAdapterService nodeRedAdapterService;
    private String templateName;
    @Autowired
    protected NodeFlowMapper nodeFlowMapper;
    @Autowired
    protected NodeFlowModelMapper nodeFlowModelMapper;

    public AbstractFlowApiService(NodeRedAdapterService nodeRedAdapterService, String templateName) {
        this.nodeRedAdapterService = nodeRedAdapterService;
        this.templateName = templateName;
    }

    @Transactional(timeout = 600)
    public void importFlow(ExportNodeFlowDto reqDto) {
        if (reqDto.getFlows() == null || reqDto.getFlows().isEmpty()) {
            return;
        }
        Set<Long> pids = reqDto.getFlows().stream().map(NodeFlowDto::getId).collect(Collectors.toSet());
        Set<String> flowIds = reqDto.getFlows().stream().map(NodeFlowDto::getFlowId).filter(Objects::nonNull).collect(Collectors.toSet());
        // 先根据批量id删除
        nodeFlowMapper.batchDeleteByIds(pids);
        if (reqDto.getFlowRefs() != null && !reqDto.getFlowRefs().isEmpty()) {
            Set<String> aliasList = reqDto.getFlowRefs().stream().map(NodeFlowModelDto::getAlias).collect(Collectors.toSet());
            nodeFlowModelMapper.deleteByAliases(aliasList);
        }
        // 重新组装组态数据
        JSONArray allFlowConfigs = null;
        if (reqDto.getNodes() != null && !reqDto.getNodes().isEmpty()) {
            // 重新组装组态数据
            allFlowConfigs = nodeRedAdapterService.retrieveAllFlowConfig();
            if (allFlowConfigs == null) {
                throw new BuzException("获取node-red组态数据失败");
            }
            List<String> globalNodeIds = new ArrayList<>();
            if (reqDto.getGlobalNodes() != null && !reqDto.getGlobalNodes().isEmpty()) {
                for (int i = 0; i < reqDto.getGlobalNodes().size(); i++) {
                    String gid = reqDto.getGlobalNodes().getJSONObject(i).getString("id");
                    globalNodeIds.add(gid);
                }
            }
            for (int i = allFlowConfigs.size() - 1; i >= 0; i--) {
                JSONObject obj = allFlowConfigs.getJSONObject(i);
                String id = obj.getString("id");
                String z = obj.getString("z");
                // 删除已经存在的流程
                if (flowIds.contains(id) || flowIds.contains(z) || globalNodeIds.contains(id)) {
                    allFlowConfigs.remove(i);
                }
            }
            allFlowConfigs.addAll(reqDto.getNodes());
            if (!globalNodeIds.isEmpty()) {
                allFlowConfigs.addAll(reqDto.getGlobalNodes());
            }
            if (reqDto.getTags() != null && !reqDto.getTags().isEmpty()) {
                // 导入流程引用的文件属性
                nodeRedAdapterService.saveRefTags(reqDto.getTags());
            }
        }
        List<NodeFlowPO> flowPOS = dto2Po(reqDto.getFlows());
        nodeFlowMapper.insert(flowPOS);
        if (reqDto.getFlowRefs() != null && !reqDto.getFlowRefs().isEmpty()) {
            List<NodeFlowModelPO> flowModelPOS = dto3Po(reqDto.getFlowRefs());
            nodeFlowModelMapper.batchInsert(flowModelPOS);
        }
        // 部署组态
        if (allFlowConfigs != null) {
            nodeRedAdapterService.deployAllToNodeRed(allFlowConfigs);
        }
    }

    protected List<String> filterSupModelNodeId(JSONArray flowNodes) {
        List<String> supmodelNodes = new ArrayList<>();
        for (int i = 0; i < flowNodes.size(); i++) {
            String type = flowNodes.getJSONObject(i).getString("type");
            if ("supmodel".equals(type)) {
                String nodeId = flowNodes.getJSONObject(i).getString("id");
                supmodelNodes.add(nodeId);
            }
        }
        return supmodelNodes;
    }

    private List<NodeFlowPO> dto2Po(List<NodeFlowDto> dtoList) {
        List<NodeFlowPO> poList = new ArrayList<>();
        for (NodeFlowDto dto : dtoList) {
            NodeFlowPO po = new NodeFlowPO();
            po.setFlowId(dto.getFlowId());
            po.setId(dto.getId());
            po.setFlowName(dto.getFlowName());
            po.setDescription(dto.getDescription());
            if (StringUtils.isEmpty(dto.getFlowId())) {
                po.setFlowStatus(FlowStatus.DRAFT.name());
            } else {
                po.setFlowStatus(FlowStatus.RUNNING.name());
            }
            po.setTemplate(templateName);
            if (UserContext.get() != null) {
                po.setCreator(UserContext.get().getPreferredUsername());
            }
            poList.add(po);
        }
        return poList;
    }

    private List<NodeFlowModelPO> dto3Po(List<NodeFlowModelDto> dtoList) {
        List<NodeFlowModelPO> poList = new ArrayList<>();
        for (NodeFlowModelDto dto : dtoList) {
            NodeFlowModelPO po = new NodeFlowModelPO();
            po.setAlias(dto.getAlias());
            po.setParentId(dto.getPid());
            poList.add(po);
        }
        return poList;
    }

    protected List<NodeFlowDto> po2Dto(List<NodeFlowPO> flowRecodes) {
        List<NodeFlowDto> dtoList = new ArrayList<>();
        for (NodeFlowPO po : flowRecodes) {
            NodeFlowDto dto = new NodeFlowDto();
            dto.setId(po.getId());
            dto.setFlowId(po.getFlowId());
            dto.setFlowName(po.getFlowName());
            dto.setDescription(po.getDescription());
            dto.setTemplate(po.getTemplate());
            dtoList.add(dto);
        }
        return dtoList;
    }

    protected void fillNodes(JSONArray flowNodes, JSONArray globalNodes) {
        // 获取所有的流程配置
        JSONArray allFlowConfigs = nodeRedAdapterService.retrieveAllFlowConfig();
        if (allFlowConfigs == null) {
            return;
        }
        for (int i = 0; i < allFlowConfigs.size(); i++) {
            String z = allFlowConfigs.getJSONObject(i).getString("z");
            String type = allFlowConfigs.getJSONObject(i).getString("type");
            // 筛选出全局节点
            if (StringUtils.isEmpty(z) && !"tab".equals(type)) {
                globalNodes.add(allFlowConfigs.getJSONObject(i));
            } else {
                flowNodes.add(allFlowConfigs.getJSONObject(i));
            }
        }
    }

    protected List<NodeFlowModelDto> po3Dto(List<NodeFlowModelPO> flowModelPOS) {
        List<NodeFlowModelDto> dtoList = new ArrayList<>();
        for (NodeFlowModelPO po : flowModelPOS) {
            NodeFlowModelDto dto = new NodeFlowModelDto();
            dto.setPid(po.getParentId());
            dto.setAlias(po.getAlias());
            dtoList.add(dto);
        }
        return dtoList;
    }
}
