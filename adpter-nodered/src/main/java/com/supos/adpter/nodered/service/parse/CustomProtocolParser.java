package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supos.adpter.nodered.dao.mapper.NodeServerMapper;
import com.supos.adpter.nodered.dao.po.IOTProtocolPO;
import com.supos.adpter.nodered.service.ObjectCachePool;
import com.supos.adpter.nodered.service.ProtocolServerService;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.protocol.MappingDTO;
import com.supos.common.exception.NodeRedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


/**
 * 处理自定义协议模版
 */
@Service("customProtocolParser")
public class CustomProtocolParser extends ParserApi {

    private String tplFile = "/custom-protocol.json.tpl";

    @Autowired
    private ObjectCachePool objectCachePool;
    @Autowired
    private NodeServerMapper nodeServerMapper;
    @Autowired
    private ProtocolServerService protocolServerService;

    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        Object inputs = uns.getConfig().get("inputs");
        // 根据input=0或者1判断启用是否带inject节点的模版
        if (inputs != null && Integer.parseInt(inputs.toString()) > 0) {
            tplFile = "/custom-protocol-with-inject.json.tpl";
        }
        return readFromTpl(tplFile);

    }

    private JSONObject rebuildClientNode(Map<String, Object> clientMap, String serverConn, String serverId, String supModelId, String customProtocolNodeId) {
        clientMap.remove("server");
        // 关联server
        clientMap.put(serverConn, serverId);
        clientMap.put("x", 610);
        clientMap.put("y", 80);
        clientMap.put("id", customProtocolNodeId);
        clientMap.put("z", "");
        Object outputs = clientMap.get("outputs");
        int outs = outputs == null ? 1 : Integer.parseInt(outputs.toString());
        String[][] wires = new String[outs][1];
        wires[0][0] = supModelId;
        clientMap.put("wires", wires);
        clientMap.remove("inputs");
        clientMap.remove("outputs");
        return JSON.parseObject(JSON.toJSONString(clientMap));
    }

    private JSONObject rebuildServerNode(String serverConfigString, String serverId) {
        JSONObject serverNode = JSON.parseObject(serverConfigString);
        serverNode.put("id", serverId);
        return serverNode;
    }



    @Override
    public void parse(String tplJsonString, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {

        int maxHeight = super.getMaxHeight(fullNodes);

        IOTProtocolPO iotProtocol = objectCachePool.getProtocolByName(uns.getProtocol());
        Object serverName = uns.getConfig().get("serverName");
        Object serverConfig = uns.getConfig().remove("server");
        if (serverConfig == null) {
            throw new NodeRedException("server配置为空");
        }
        if (serverName == null) {
            serverName = protocolServerService.guessServerNameFromConfig((Map<String, Object>)serverConfig);
        }
        String serverConfigJson = JSON.toJSONString(serverConfig);

        String serverId = super.createServer(serverName.toString(), uns.getProtocol(), serverConfigJson);
        // 获取inject节点，用于判断引用的是哪个流程模版
        JSONObject injectNode = getInjectByServerId(fullNodes, serverId);
        JSONObject mc = buildPayloadString(uns.getUnsTopic());
        Map<String, List<MappingDTO>> ms = buildMapping(uns.getFields(), uns.getUnsTopic());

        if (injectNode != null) {
            String payloadString = injectNode.getString("payload");
            JSONArray payloadArray = JSONArray.parseArray(payloadString);
            payloadArray.add(mc);
            injectNode.put("payload", payloadArray.toJSONString());

            JSONObject supModelNode = getSupModelByServerId(fullNodes, serverId);
            Map<String, List<MappingDTO>> nowMap = supModelNode.getObject("mappings", Map.class);
            for (Map.Entry<String, List<MappingDTO>> entry : ms.entrySet()) {
                List<MappingDTO> tmp = nowMap.get(entry.getKey());
                if (tmp == null) {
                    nowMap.put(entry.getKey(), entry.getValue());
                } else {
                    tmp.addAll(entry.getValue());
                    nowMap.put(entry.getKey(), tmp);
                }
            }
            supModelNode.put("mappings", nowMap);
            return;
        }

        // 判断mqtt-broker节点是否存在，如果存在删除它
        JSONObject mqttBrokerNode = getMqttBrokerNode(fullNodes);
        if (mqttBrokerNode != null) {
            fullNodes.remove(mqttBrokerNode);
        }

        String supModelId = IDGenerator.generate();
        String injectId = IDGenerator.generate();
        String mqttNodeId = IDGenerator.generate();
        String functionId = IDGenerator.generate();
        String customProtocolNodeId = IDGenerator.generate();

        // 替换节点id
        String jsonFlowStr = tplJsonString.replaceAll("\\$id_model_selector", supModelId)
                .replaceAll("\\$id_mqtt", mqttNodeId)
                .replaceAll("\\$id_custom_protocol_server", serverId)
                .replaceAll("\\$id_inject", injectId)
                .replaceAll("\\$id_custom_protocol_node", customProtocolNodeId)
                .replaceAll("\\$id_func", functionId);

        // 将modbus client配置动态放入inject中
        JSONArray payloads = new JSONArray();
        payloads.add(mc);
        String configString = payloads.toJSONString().replace("\"", "\\\"");
        jsonFlowStr = jsonFlowStr.replace("$payload_json_array", configString);

        JSONArray jsonArr = JSON.parseArray(jsonFlowStr);
        // 动态添加节点
        JSONObject clientNode = rebuildClientNode(uns.getConfig(), iotProtocol.getServerConn(), serverId, supModelId, customProtocolNodeId);
        JSONObject serverNode = rebuildServerNode(serverConfigJson, serverId);
        jsonArr.add(clientNode);
        jsonArr.add(serverNode);
        // 设置节点高度
        for (int i = 0; i < jsonArr.size(); i++) {
            Integer highSpace = jsonArr.getJSONObject(i).getInteger("iy");
            int y = maxHeight + intervalHeight;
            if (highSpace != null) {
                y += highSpace;
            }
            jsonArr.getJSONObject(i).put("y", y);
            String nodeType = jsonArr.getJSONObject(i).getString("type");
            if ("supmodel".equals(nodeType)) {
                jsonArr.getJSONObject(i).put("mappings", ms);
                JSONArray models = jsonArr.getJSONObject(i).getJSONArray("models");
                models.add(uns.getModel());
            }
        }

        fullNodes.addAll(jsonArr);
    }

    private JSONObject getMqttBrokerNode(JSONArray fullNodes) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if ("mqtt-broker".equals(nodeType)) {
                return fullNodes.getJSONObject(i);
            }
        }
        return null;
    }

    private JSONObject getSupModelByServerId(JSONArray fullNodes, String serverId) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if ("supmodel".equalsIgnoreCase(nodeType)) {
                String sid = fullNodes.getJSONObject(i).getString("id_server");
                if (serverId.equals(sid)) {
                    return fullNodes.getJSONObject(i);
                }
            }
        }
        return null;
    }

    private JSONObject getInjectByServerId(JSONArray fullNodes, String serverId) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if ("inject".equalsIgnoreCase(nodeType)) {
                String sid = fullNodes.getJSONObject(i).getString("id_server");
                if (serverId.equals(sid)) {
                    return fullNodes.getJSONObject(i);
                }
            }
        }
        return null;
    }

    private JSONObject buildPayloadString(String topic) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", topic);
        return jsonObject;
    }
}
