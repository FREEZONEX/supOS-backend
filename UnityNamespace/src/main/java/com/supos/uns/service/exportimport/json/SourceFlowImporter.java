package com.supos.uns.service.exportimport.json;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.supos.adpter.nodered.dao.mapper.NodeFlowMapper;
import com.supos.adpter.nodered.dao.mapper.NodeFlowModelMapper;
import com.supos.adpter.nodered.dao.po.NodeFlowModelPO;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import com.supos.common.dto.NodeRedTagsDTO;
import com.supos.common.enums.GlobalExportModuleEnum;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.NodeRedUtils;
import com.supos.uns.service.exportimport.core.SourceFlowImportContext;
import com.supos.uns.service.exportimport.json.data.SourceFlowJsonWrapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月21日 14:32
 */
@Slf4j
public class SourceFlowImporter {
    public SourceFlowImporter(SourceFlowImportContext context, NodeFlowMapper nodeFlowMapper, NodeFlowModelMapper nodeFlowModelMapper) {
        this.context = context;
        this.nodeFlowMapper = nodeFlowMapper;
        this.nodeFlowModelMapper = nodeFlowModelMapper;
    }

    @Getter
    private StopWatch stopWatch = new StopWatch();
    private SourceFlowImportContext context;
    private SourceFlowJsonWrapper sourceFlowJsonWrapper;
    private NodeFlowMapper nodeFlowMapper;
    private NodeFlowModelMapper nodeFlowModelMapper;

    public void importData(File file) {
        try {
            JsonMapper jsonMapper = new JsonMapper();
            sourceFlowJsonWrapper = jsonMapper.readValue(file, SourceFlowJsonWrapper.class);
        } catch (Exception e) {
            log.error("解析json文件失败", e);
            throw new BuzException("sourceFlow.import.json.error");
        }
        try {
            handleImportData(sourceFlowJsonWrapper);
            log.info("sourceFlow import running time:{}s", stopWatch.getTotalTimeSeconds());
            log.info(stopWatch.prettyPrint());
        } catch (Exception e) {
            log.error("导入失败", e);
            throw new BuzException("sourceFlow.import.error");
        }
    }

    private void handleImportData(SourceFlowJsonWrapper sourceFlowJsonWrapper) {
        if (sourceFlowJsonWrapper == null || (CollUtil.isEmpty(sourceFlowJsonWrapper.getFlows()) && CollUtil.isEmpty(sourceFlowJsonWrapper.getFlowModels()))) {
            return;
        }
        context.setTotal(sourceFlowJsonWrapper.getFlows().size());
        Date now = new Date();
        List<String> flowIds = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (NodeFlowPO flow : sourceFlowJsonWrapper.getFlows()) {
            flow.setCreateTime(now);
            flow.setUpdateTime(now);
            names.add(flow.getFlowName());
            if (StringUtils.hasText(flow.getFlowId())) {
                flowIds.add(flow.getFlowId());
            }
        }
        Map<Long, List<NodeFlowModelPO>> nodeFlowModelMap = new HashMap<>();
        if (CollUtil.isNotEmpty(sourceFlowJsonWrapper.getFlowModels())) {
            for (NodeFlowModelPO modelFLow : sourceFlowJsonWrapper.getFlowModels()) {
                nodeFlowModelMap.computeIfAbsent(modelFLow.getParentId(), v -> new ArrayList<>()).add(modelFLow);
            }
        }
        Map<String, NodeFlowPO> existMap = new HashMap<>();
        Map<String,NodeFlowPO> existNameMap = new HashMap<>();
        if(CollUtil.isNotEmpty(flowIds)){
            List<NodeFlowPO> nodeFlowPOS = nodeFlowMapper.selectByFlowIds(flowIds);
            if (CollUtil.isNotEmpty(nodeFlowPOS)) {
                for (NodeFlowPO nodeFlowPO : nodeFlowPOS) {
                    existMap.put(nodeFlowPO.getFlowId(), nodeFlowPO);
                }
            }
        }
        List<NodeFlowPO> nodeFlowPOS = nodeFlowMapper.selectByFlowNames(names);
        if (CollUtil.isNotEmpty(nodeFlowPOS)) {
            for (NodeFlowPO nodeFlowPO : nodeFlowPOS) {
                existNameMap.put(nodeFlowPO.getFlowName(),nodeFlowPO);
            }
        }
        List<NodeFlowPO> addList = new ArrayList<>();
        List<NodeFlowModelPO> addModelList = new ArrayList<>();
        for (NodeFlowPO nodeFlowPO : sourceFlowJsonWrapper.getFlows()) {
            if (existMap.containsKey(nodeFlowPO.getFlowId())) {
                context.addError(nodeFlowPO.getFlowId(), I18nUtils.getMessage("sourceFlow.id.already.exists"));
            }else if(existNameMap.containsKey(nodeFlowPO.getFlowName())){
                context.addError(nodeFlowPO.getFlowId(), I18nUtils.getMessage("sourceFlow.name.already.exists"));
            } else {
                addList.add(nodeFlowPO);
                List<NodeFlowModelPO> nodeFlowModelPOS = nodeFlowModelMap.get(nodeFlowPO.getId());
                if (CollUtil.isNotEmpty(nodeFlowModelPOS)) {
                    addModelList.addAll(nodeFlowModelPOS);
                }
                nodeFlowPO.setId(IdUtil.getSnowflakeNextId());
                if (CollUtil.isNotEmpty(nodeFlowModelPOS)) {
                    for (NodeFlowModelPO nodeFlowModelPO : nodeFlowModelPOS) {
                        nodeFlowModelPO.setParentId(nodeFlowPO.getId());
                    }
                }
            }
        }
        if (CollUtil.isNotEmpty(addList)) {
            for (NodeFlowPO nodeFlowPO : addList) {
                if (StringUtils.hasText(nodeFlowPO.getFlowData())) {
                    String flowId = NodeRedUtils.create(GlobalExportModuleEnum.SOURCE_FLOW, null, nodeFlowPO.getFlowName(), nodeFlowPO.getDescription(), null, context.getNodeRedHost(), context.getNodeRedPort());
                    nodeFlowPO.setFlowId(flowId);
                    JSONArray nodes = JSON.parseArray(nodeFlowPO.getFlowData());
                    NodeRedUtils.create(GlobalExportModuleEnum.SOURCE_FLOW, nodeFlowPO.getFlowId(), nodeFlowPO.getFlowName(), nodeFlowPO.getDescription(), nodes, context.getNodeRedHost(), context.getNodeRedPort());
                    JSONObject nodeTags = nodeFlowPO.getNodeTags();
                    // 更新节点的z属性（流程ID）
                    for (int i = 0; i < nodes.size(); i++) {
                        String parentId = nodes.getJSONObject(i).getString("z");
                        if (parentId != null) {
                            nodes.getJSONObject(i).put("z", flowId);
                        }
                        String nodeId = nodes.getJSONObject(i).getString("id");
                        if(nodeTags != null && nodeTags.get(nodeId) != null){
                            NodeRedTagsDTO nodeRedTagsDTO = nodeTags.getObject(nodeId, NodeRedTagsDTO.class);
                            if(nodeRedTagsDTO != null && CollUtil.isNotEmpty(nodeRedTagsDTO.getData())){
                                NodeRedUtils.saveTags(nodeId,nodeRedTagsDTO.getData(),context.getNodeRedHost(),context.getNodeRedPort());
                            }
                        }
                    }
                }
            }
            nodeFlowMapper.insert(addList);
            if (CollUtil.isNotEmpty(addModelList)) {
                nodeFlowModelMapper.batchInsert(addModelList);
            }
        }
    }

    public void writeError(File outFile) {
        try {
            JsonFactory factory = new JsonMapper().getFactory();
            JsonGenerator jsonGenerator = factory.createGenerator(outFile, JsonEncoding.UTF8);
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeStartObject();
            // 导出模板
            jsonGenerator.writeFieldName("flows");
            jsonGenerator.writeStartArray();
            List<NodeFlowPO> flows = sourceFlowJsonWrapper.getFlows();
            if (CollectionUtils.isNotEmpty(flows)) {
                for (NodeFlowPO flowPO : flows) {
                    flowPO.setError(context.getCheckErrorMap().get(flowPO.getFlowId()));
                    jsonGenerator.writePOJO(flowPO);
                }
            }
            jsonGenerator.writeEndArray();

            List<NodeFlowModelPO> flowModels = sourceFlowJsonWrapper.getFlowModels();
            if (CollectionUtils.isNotEmpty(flowModels)) {
                jsonGenerator.writeFieldName("flowModels");
                jsonGenerator.writeStartArray();
                for (NodeFlowModelPO flowModelPO : flowModels) {
                    jsonGenerator.writePOJO(flowModelPO);
                }
                jsonGenerator.writeEndArray();
            }
            jsonGenerator.writeEndObject();
            jsonGenerator.close();
        } catch (Exception e) {
            log.error("EventFlow导入失败", e);
            throw new BuzException(e.getMessage());
        }
    }
}
