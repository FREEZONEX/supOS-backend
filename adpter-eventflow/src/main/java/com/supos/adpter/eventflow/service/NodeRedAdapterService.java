package com.supos.adpter.eventflow.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supos.adpter.eventflow.dao.mapper.EventFlowMapper;
import com.supos.adpter.eventflow.dao.mapper.EventFlowModelMapper;
import com.supos.adpter.eventflow.dao.po.NodeFlowModelPO;
import com.supos.adpter.eventflow.dao.po.NodeFlowPO;
import com.supos.adpter.eventflow.enums.FlowStatus;
import com.supos.adpter.eventflow.util.IDGenerator;
import com.supos.adpter.eventflow.vo.NodeFlowVO;
import com.supos.adpter.eventflow.vo.UpdateFlowRequestVO;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.exception.NodeRedException;
import com.supos.common.utils.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service("EventFlowNodeRedAdapterService")
@Slf4j
public class NodeRedAdapterService {

    private static final String nodeRedHost = "eventflow";
    private static final String nodeRedPort = "1889";
    public String getNodeRedHost(){
        if (RuntimeUtil.isLocalProfile()) {
            return "http://100.100.100.22:33893/eventflow/home";
        } else {
            return nodeRedHost;
        }
    }
    public String getNodeRedPort(){
        if (RuntimeUtil.isLocalProfile()) {
            return "";
        } else {
            return nodeRedPort;
        }
    }
    @Autowired
    private EventFlowMapper nodeFlowMapper;
    @Autowired
    private EventFlowModelMapper nodeFlowModelMapper;

    /**
     * proxy nodered /flows api
     * @return json string
     */
    public JSONObject proxyGetFlow(long id) {
        // cookie中不带flowId，返回空列表
        if (id == 0) {
            throw new NodeRedException(400, "nodered.flowId.not.exist");
        }
        NodeFlowPO nodeFlow = nodeFlowMapper.getById(id);
        if (nodeFlow == null) {
            throw new NodeRedException(400, "nodered.flow.not.exist");
        }
        String flowJson = nodeFlow.getFlowData();
        // 当flowId存在且flowData不存在， 需要调用node-red服务，检查是否在服务端运行
        /*if (StringUtils.hasText(nodeFlow.getFlowId()) && !StringUtils.hasText(flowJson)) {
            flowJson = getFlowDataFromNodeRed(nodeFlow.getFlowId());
            // update to db
            if (StringUtils.hasText(flowJson)) {
                nodeFlowMapper.deployUpdate(id, nodeFlow.getFlowId(), FlowStatus.RUNNING.name(), flowJson);
            }
        }*/
        JSONArray nodes = StringUtils.hasText(flowJson) ? JSON.parseArray(flowJson) : new JSONArray();
        addLabelNode(nodes, nodeFlow.getFlowId(), nodeFlow.getFlowName(), nodeFlow.getDescription());
        // 添加全局节点
        addGlobalNode(nodes);
        JSONObject response = new JSONObject();
        response.put("flows", nodes);
        return response;
    }

    private void addGlobalNode(JSONArray nodes) {
        JSONArray globalNodes = retrieveGlobalNodeFromNodeRed();
        if (globalNodes == null) {
            return;
        }
        for (int i = 0; i < globalNodes.size(); i++) {
            String gid = globalNodes.getJSONObject(i).getString("id");
            boolean bingo = true;
            for (int j = 0; j < nodes.size(); j++) {
                String id = nodes.getJSONObject(j).getString("id");
                if (gid.equals(id)) {
                    bingo = false;
                    break;
                }
            }
            if (bingo) {
                nodes.add(globalNodes.getJSONObject(i));
            }
        }
    }

    /**
     * 根据topic获取对应流程
     * @param topic
     * @return
     */
    public List<NodeFlowVO> getByTopic(String topic) {
        List<Long> flowIds = nodeFlowModelMapper.queryByTopic(topic);
        if (flowIds != null && flowIds.size() > 0) {
            List<NodeFlowPO> nodeFlows = nodeFlowMapper.selectByIds(flowIds);
            return buildNodeFlowVOs(nodeFlows);
        }
        return new ArrayList<>(1);
    }

    /**
     * 直接走node-red服务
     * @return
     */
    public JSONObject getFromNodeRed() {
        HttpRequest getClient = HttpUtil.createGet(String.format("http://%s:%s/flows", nodeRedHost, nodeRedPort));
        HttpResponse response = getClient.execute();
        return JSON.parseObject(response.body());
    }

    /**
     * 分页查询， 支持根据名称模糊搜索
     * @param fuzzyName
     * @param pageNo
     * @param pageSize
     * @return
     */
    public PageResultDTO<NodeFlowVO> selectList(String fuzzyName, int pageNo, int pageSize) {
        PageResultDTO.PageResultDTOBuilder<NodeFlowVO> pageBuilder = PageResultDTO.<NodeFlowVO>builder().pageNo(pageNo).pageSize(pageSize);
        int total = nodeFlowMapper.selectTotal(fuzzyName);
        if (total == 0) {
            return pageBuilder.code(200).data(new ArrayList<>(1)).build();
        }
        List<NodeFlowPO> nodeFlowList = nodeFlowMapper.selectFlows(fuzzyName, pageNo, pageSize);
        List<NodeFlowVO> nodeFlowVOS = buildNodeFlowVOs(nodeFlowList);
        return pageBuilder.code(200).total(total).data(nodeFlowVOS).build();
    }

    private List<NodeFlowVO> buildNodeFlowVOs(List<NodeFlowPO> nodeFlowPOS) {
        List<NodeFlowVO> vos = new ArrayList<>();
        for (NodeFlowPO po : nodeFlowPOS) {
            NodeFlowVO vo = buildNodeFlowVO(po);
            vos.add(vo);
        }
        return vos;
    }

    private NodeFlowVO buildNodeFlowVO(NodeFlowPO po) {
        NodeFlowVO vo = new NodeFlowVO();
        vo.setFlowId(po.getFlowId());
        vo.setDescription(po.getDescription());
        vo.setFlowStatus(po.getFlowStatus());
        vo.setTemplate(po.getTemplate());
        vo.setId(po.getId() + "");
        vo.setFlowName(po.getFlowName());
        return vo;
    }

    /**
     *
     * @param nodes
     * @return flowId
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
    public String proxyDeploy(long id, JSONArray nodes, List<String> topics) {
        NodeFlowPO nodeFlow = nodeFlowMapper.getById(id);
        if (nodeFlow == null) {
           throw new NodeRedException(400, "nodered.flow.not.exist");
        }
        String flowId = nodeFlow.getFlowId();
        if (flowId == null) {
            // 全新部署, 先创建一个空的流程，避免节点冲突
            flowId = deployToNodeRed("", nodeFlow.getFlowName(), nodeFlow.getDescription(), null);
        }
        // 拆分流程节点和全局节点
        JSONArray globalNodes = filterGlobalNodes(nodes);
        // 更新部署
        deployToNodeRed(flowId, nodeFlow.getFlowName(), nodeFlow.getDescription(), nodes);

        deployGlobalNodesToNodeRed(globalNodes);

        // 更新节点的z属性（流程ID）
        for (int i = 0; i < nodes.size(); i++) {
            String parentId = nodes.getJSONObject(i).getString("z");
            if (parentId != null) {
                nodes.getJSONObject(i).put("z", flowId);
            }
        }
        // 记录流程和uns模型的关联关系
        List<NodeFlowModelPO> flowModels = new ArrayList<>();
        if (topics == null || topics.isEmpty()) {
            flowModels = parseTopicFromFlow(id, nodes);
        } else {
            for (String t : topics) {
                flowModels.add(new NodeFlowModelPO(id, t));
            }
        }
        // update database
        nodeFlowMapper.deployUpdate(id, flowId, FlowStatus.RUNNING.name(), nodes.toString());
        nodeFlowModelMapper.deleteById(id);
        if (!flowModels.isEmpty()) {
            List<List<NodeFlowModelPO>> lists = cutList(flowModels);
            for (List<NodeFlowModelPO> list : lists) {
                nodeFlowModelMapper.batchInsert(list);
            }
        }
        return flowId;
    }

    // 从当前节点列表中过滤出全局节点，并返回全局节点列表
    private JSONArray filterGlobalNodes(JSONArray nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        JSONArray globalNodes = new JSONArray();
        Iterator iterator = nodes.iterator();
        while(iterator.hasNext()) {
            Map<String, Object> obj = (Map)iterator.next();
            Object type = obj.get("type");
            Object z = obj.get("z");
            if (!"tab".equals(type.toString()) && z == null) {
                globalNodes.add(obj);
                iterator.remove();
            }
        }
        return globalNodes;
    }

    List<NodeFlowModelPO> parseTopicFromFlow(long id, JSONArray nodes) {
        List<NodeFlowModelPO> flowModels = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            String nodeType = nodes.getJSONObject(i).getString("type");
            String modelTopic = nodes.getJSONObject(i).getString("selectedModel");
            // 统计关联了哪些模型topic
            if ("supmodel".equals(nodeType) && !"Auto".equals(modelTopic)) {
                flowModels.add(new NodeFlowModelPO(id, modelTopic));
            } else if ("modelConverter".equals(nodeType) ) {
                flowModels.add(new NodeFlowModelPO(id, modelTopic));
            } else if ("inject".equals(nodeType)) {
                // 解析inject payload, 这里的解析过程需要知晓inject节点的数据结构
                JSONArray payload = null;
                try {
                    payload = nodes.getJSONObject(i).getJSONArray("payload");
                } catch (Exception ignore) {
                    // ignore
                }
                if (payload != null) {
                    for (int j = 0; j < payload.size(); j++) {
                        String topic  = payload.getJSONObject(j).getString("model");
                        flowModels.add(new NodeFlowModelPO(id, topic));
                    }
                }
            }
        }
        return flowModels;
    }

    private List<List<NodeFlowModelPO>> cutList(List<NodeFlowModelPO> totalList) {
        int totalSize = totalList.size();
        List<List<NodeFlowModelPO>> splitLists = new ArrayList<>();

        if (totalSize <= 1000) {
            splitLists.add(totalList);
            return splitLists;
        }
        // 按照1000切割
        for (int i = 0; i < totalSize; i += 1000) {
            int end = Math.min(i + 1000, totalSize);
            splitLists.add(totalList.subList(i, end));
        }

        return splitLists;
    }

    /**
     * 新建流程
     * @param flowName 流程名称
     * @param description
     * @param template 模版来源
     * @return id
     */
    public long createFlow(String flowName, String description, String template) {
        // 判断流程是否存在
        NodeFlowPO nf = nodeFlowMapper.getByName(flowName);
        if (nf != null) {
            throw new NodeRedException(400, "nodered.flowName.duplicate");
        }
        NodeFlowPO flowPO = new NodeFlowPO();
        flowPO.setId(IdUtil.getSnowflakeNextId());
        flowPO.setFlowStatus(FlowStatus.DRAFT.name());
        flowPO.setFlowName(flowName);
        flowPO.setDescription(description);
        flowPO.setTemplate(template);
        nodeFlowMapper.insert(flowPO);
        return flowPO.getId();
    }

    /**
     * 复制流程 待发布的新流程
     * @param sourceId 需要复制的原始流程ID
     * @param flowName
     * @param description
     * @param template
     * @return
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
    public long copyFlow(String sourceId, String flowName, String description, String template) {
        NodeFlowPO nodeFlow = nodeFlowMapper.getById(Long.parseLong(sourceId));
        if (nodeFlow == null) {
            throw new NodeRedException(400, "nodered.flow.not.exist");
        }
        long id = createFlow(flowName, description, template);
        if (StringUtils.hasText(nodeFlow.getFlowData())) {
            JSONArray nodes = JSON.parseArray(nodeFlow.getFlowData());
            // 变更节点id
            String nodesString = JSON.toJSONString(nodes);
            for (int i = 0; i < nodes.size(); i++) {
                String z = nodes.getJSONObject(i).getString("z");
                if (StringUtils.hasText(z)) { // 只修改流程范围内的节点ID，全局节点不修改
                    String newId = IDGenerator.generate();
                    String oldId = nodes.getJSONObject(i).getString("id");
                    nodesString = nodesString.replaceAll(oldId, newId);
                }
            }
            JSONArray newNodes = JSON.parseArray(nodesString);
            saveFlowData(id, newNodes);
        }
        return id;
    }


    /**
     * 保存草稿
     * @param id
     * @param nodes nodes exclude label
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
    public void saveFlowData(long id, JSONArray nodes) {
        NodeFlowPO nodeFlow = nodeFlowMapper.getById(id);
        if (nodeFlow == null) {
            throw new NodeRedException(400, "nodered.flow.not.exist");
        }
        String flowJson = "";
        List<NodeFlowModelPO> flowModels = new ArrayList<>();
        if (nodes != null) {
            flowJson = nodes.toString();
            for (int i = 0; i < nodes.size(); i++) {
                String nodeType = nodes.getJSONObject(i).getString("type");
                // 统计关联了哪些模型topic
                if ("model-selector".equals(nodeType)) {
                    String topic = nodes.getJSONObject(i).getString("selectedModel");
                    flowModels.add(new NodeFlowModelPO(id, topic));
                }
            }
        }
        String status = StringUtils.hasText(nodeFlow.getFlowId()) ? FlowStatus.PENDING.name() : FlowStatus.DRAFT.name();
        nodeFlowMapper.saveFlowData(id, status, flowJson);
        /*nodeFlowModelMapper.deleteById(id);
        if (!flowModels.isEmpty()) {
            nodeFlowModelMapper.batchInsert(flowModels);
        }*/
    }

    /**
     * update flow basic info, example: name、description
     * @param requestVO
     */
    public void updateFlow(UpdateFlowRequestVO requestVO) {
        long id = Long.parseLong(requestVO.getId());
        NodeFlowPO nodeFlow = nodeFlowMapper.getById(id);
        if (nodeFlow == null) {
            throw new NodeRedException(400, "nodered.flow.not.exist");
        }
        // 验证名称是否被占用
        if (!nodeFlow.getFlowName().equals(requestVO.getFlowName())) {
            NodeFlowPO flowPO = nodeFlowMapper.getByName(requestVO.getFlowName());
            if (flowPO != null) {
                throw new NodeRedException(400, "nodered.flowName.has.used");
            }
        }
        if (StringUtils.hasText(nodeFlow.getFlowId())) {
            JSONArray flowNodes = StringUtils.hasText(nodeFlow.getFlowData()) ? JSON.parseArray(nodeFlow.getFlowData()) : new JSONArray();
            // update node-red
            deployToNodeRed(nodeFlow.getFlowId(), requestVO.getFlowName(), requestVO.getDescription(), flowNodes);
        }
        // update db
        nodeFlowMapper.updateBasicInfoById(id, requestVO.getFlowName(), requestVO.getDescription());
    }

    /**
     * 删除流程，在node-red的数据也一并删除
     * @param id
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
    public void deleteFlow(long id, boolean throwEx) {
        NodeFlowPO nodeFlow = nodeFlowMapper.getById(id);
        if (nodeFlow == null) {
            if (throwEx) {
                throw new NodeRedException(400, "nodered.flow.not.exist");
            }
            return;
        }
        if (StringUtils.hasText(nodeFlow.getFlowId())) {
            deleteFromNodeRed(nodeFlow.getFlowId());
        }
        nodeFlowMapper.deleteById(id);
        nodeFlowModelMapper.deleteById(id);
    }

    private void deleteFromNodeRed(String flowId) {
        String url = String.format("http://%s:%s/flow/%s", nodeRedHost, nodeRedPort, flowId);
        HttpRequest request = HttpUtil.createRequest(Method.DELETE, url);
        HttpResponse response = request.execute();
        if (!isSuccess(response.getStatus()) && response.getStatus() != 404) {
            throw new NodeRedException(response.body());
        }
    }

    private boolean isSuccess(int code) {
        return code == 200 || code == 204;
    }


    // 不包含label节点
    private JSONArray retrieveGlobalNodeFromNodeRed() {
        HttpRequest getClient = HttpUtil.createGet(String.format("http://%s:%s/flows", nodeRedHost, nodeRedPort));
        HttpResponse response = getClient.execute();
        if (!isSuccess(response.getStatus())) {
            log.error("node-red获取全局节点失败： error = {}", response.body());
            return null;
        }
        JSONArray nodes = JSON.parseArray(response.body());
        JSONArray globalNodes = new JSONArray();
        if (nodes != null && nodes.size() > 0) {
            // 排除type=tab和z属性不为空的节点，只保留全局节点
            for (int i = 0; i < nodes.size(); i++) {
                String type = nodes.getJSONObject(i).getString("type");
                String z = nodes.getJSONObject(i).getString("z");
                if (!"tab".equals(type) && !StringUtils.hasText(z)) {
                    globalNodes.add(nodes.getJSONObject(i));
                }
            }
            return globalNodes;
        }
        return null;
    }

    // 添加label节点
    private JSONArray addLabelNode(JSONArray nodes, String flowId, String flowName, String description) {
        JSONObject labelNode = new JSONObject();
        if (StringUtils.hasText(flowId)) {
            labelNode.put("id", flowId);
        } else {
            String newFlowId = UUID.randomUUID().toString().replaceAll("-", "");
            labelNode.put("id", newFlowId);
        }
        labelNode.put("type", "tab");
        labelNode.put("label", flowName);
        labelNode.put("disabled", false);
        labelNode.put("info", description);
        nodes.add(labelNode);
        return nodes;
    }

    private void deployGlobalNodesToNodeRed(JSONArray globalNodes) {
        if (globalNodes == null || globalNodes.isEmpty()) {
            return;
        }
        JSONObject requestBody = new JSONObject();
        requestBody.put("id", "global");
        requestBody.put("configs", globalNodes);

        String url = String.format("http://%s:%s/flow/global", nodeRedHost, nodeRedPort);
        HttpRequest httpClient = HttpUtil.createRequest(Method.PUT, url);

        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json; charset=UTF-8");
        httpClient.addHeaders(headers);
        httpClient.body(requestBody.toJSONString());
        // 连接超时和读取响应超时 10分钟
        httpClient.timeout(10 * 60 * 1000);

        HttpResponse response = httpClient.execute();
        log.info("update global nodes to node-red, response: {}", response.body());
    }

    // 部署流程到node-red
    public String deployToNodeRed(String flowId, String flowName, String description, JSONArray nodes) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("id", flowId);
        requestBody.put("nodes", nodes == null ? new ArrayList<>(1) : nodes);
        requestBody.put("disabled", false);
        requestBody.put("label", flowName);
        requestBody.put("info", description);

        HttpRequest httpClient = null;
        if (StringUtils.hasText(flowId)) {
            String url = String.format("http://%s:%s/flow/%s", nodeRedHost, nodeRedPort, flowId);
            httpClient = HttpUtil.createRequest(Method.PUT, url);
        } else {
            String url = String.format("http://%s:%s/flow", nodeRedHost, nodeRedPort);
            httpClient = HttpUtil.createRequest(Method.POST, url);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json; charset=UTF-8");
        httpClient.addHeaders(headers);
        httpClient.body(requestBody.toJSONString());
        // 连接超时和读取响应超时 10分钟
        httpClient.timeout(10 * 60 * 1000);

        HttpResponse response = httpClient.execute();
//        log.info("update flow to node-red, response: {}", response.body());
        if (!isSuccess(response.getStatus())) {
            throw new NodeRedException(response.body());
        }
        // get id from response
        return JSON.parseObject(response.body()).getString("id");
    }

    /**
     * 根据topic批量查询关联流程
     * @param topics
     * @return
     */
    public List<NodeFlowVO> selectByTopics(Collection<String> topics) {
        List<Long> parentIds = nodeFlowModelMapper.selectByTopics(topics);
        List<NodeFlowVO> nodeFlowVo = new ArrayList<>();
        if (parentIds.isEmpty()) {
            return nodeFlowVo;
        }
        List<NodeFlowPO> nodeFlows = nodeFlowMapper.selectByIds(parentIds);
        for (NodeFlowPO po : nodeFlows) {
            NodeFlowVO vo = new NodeFlowVO();
            vo.setId(po.getId() + "");
            vo.setFlowName(po.getFlowName());
            nodeFlowVo.add(vo);
        }
        return nodeFlowVo;
    }




}
