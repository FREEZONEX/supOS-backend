package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.supos.adpter.nodered.dao.mapper.NodeServerMapper;
import com.supos.adpter.nodered.enums.DataType;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.protocol.OpcUAConfigDTO;
import com.supos.common.enums.FieldType;
import com.supos.common.enums.IOTProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * 解析时序模型对应的node-red模版文件
 */
@Service("opcuaParser")
public class OpcUAParser extends ParserApi {

    private String tplFile = "/opcua-read.json.tpl";

    private int intervalHeight = 40;

    @Autowired
    private NodeServerMapper nodeServerMapper;

    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        return readFromTpl(tplFile);
    }

    @Override
    public void parse(String tpl, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {
        // 将opcua的配置由key-value转成对象，方便读取
        OpcUAConfigDTO opcuaConfig = (OpcUAConfigDTO) uns.getProtocolBean();
        if (opcuaConfig == null) {
            opcuaConfig = JSON.parseObject(JSON.toJSONString(uns.getConfig()), OpcUAConfigDTO.class, Feature.TrimStringFieldValue);
        }
        String serverConfigJson = JSON.toJSONString(opcuaConfig.getServer());
        String serverId = super.createServer(opcuaConfig.getServerName(), IOTProtocol.OPC_UA.getName(), serverConfigJson);
        // 判断mqtt-broker节点是否存在，如果存在删除它
        JSONObject mqttBrokerNode = getMqttBrokerNode(fullNodes);
        if (mqttBrokerNode != null) {
            fullNodes.remove(mqttBrokerNode);
        }

        int maxHeight = super.getMaxHeight(fullNodes);

        String selectModelNodeId = IDGenerator.generate();
        String mqttNodeId = IDGenerator.generate();

        String clientId = getClientIdByServerId(fullNodes, serverId);
        String jsonFlowStr = tpl;

        Map<String, ?> ms = buildMapping(uns.getFields(), uns.getUnsTopic());

        if (clientId == null) {
            // 替换节点id
            clientId = UUID.randomUUID().toString().replaceAll("-", "");
            jsonFlowStr = jsonFlowStr.replaceAll("\\$id_model_selector", selectModelNodeId)
                    .replaceAll("\\$id_mqtt", mqttNodeId)
                    .replaceAll("\\$id_opcua_client", clientId)
                    .replaceAll("\\$model_topic", uns.getUnsTopic())
                    .replaceAll("\\$id_opcua_server", serverId);
            // 替换opcua-server endpoint
            jsonFlowStr = jsonFlowStr.replace("$opcua_server_addr", opcuaConfig.getServer().getEndpoint());
            // 替换采集频率 单位是秒
            jsonFlowStr = jsonFlowStr.replace("$pollRate", opcuaConfig.getPollRate().getSeconds() + "");

        }
        JSONArray jsonArr = JSON.parseArray(jsonFlowStr);
        // 设置整体流程高度
        int baseHeight = maxHeight + intervalHeight;
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
                jsonArr.getJSONObject(i).put("model", uns.getModel());
            }
        }
        // 根据uns配置的字段属性， 动态添加inject和opcua-item节点
        JSONArray dynamicNodes = handleDynamicNode(uns, jsonArr, clientId, baseHeight);
        // 取出不是模版节点的数据，并放到动态节点中
        for (int i = 0; i < jsonArr.size(); i++) {
            JSONObject obj = jsonArr.getJSONObject(i);
            if (!isTemplateNode(obj)) {
                dynamicNodes.add(obj);
            }
        }
        fullNodes.addAll(dynamicNodes);
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

    private String getClientIdByServerId(JSONArray fullNodes, String serverId) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String serverNodeId = fullNodes.getJSONObject(i).getString("endpoint");
            if (serverNodeId != null && serverNodeId.equals(serverId)) {
                return fullNodes.getJSONObject(i).getString("id");
            }
        }
        return null;
    }

    private JSONArray handleDynamicNode(BatchImportRequestVO.UnsVO uns, JSONArray jsonArr, String clientId, int baseHeight) {
        JSONArray itemNodeArr = new JSONArray();
        // 替换item节点ID
        for (int j = 0; j < uns.getFields().size(); j++) {
            String injectNodeId = IDGenerator.generate();
            String opcuaItemNodeId = IDGenerator.generate();
            for (int i = 0; i < jsonArr.size(); i++) {
                JSONObject node = jsonArr.getJSONObject(i);
                if (isTemplateNode(node)) {
                    // 处理inject节点
                    if ("inject".equalsIgnoreCase(node.getString("type"))) {
                        String injectNodeJsonTpl = node.toString();
                        injectNodeJsonTpl = injectNodeJsonTpl.replace("$id_inject", injectNodeId);
                        injectNodeJsonTpl = injectNodeJsonTpl.replace("$id_opcua_item", opcuaItemNodeId);
                        JSONObject newInjectNode = JSON.parseObject(injectNodeJsonTpl);
                        newInjectNode.put("y", baseHeight + 40 * j);
                        itemNodeArr.add(newInjectNode);
                    }
                    // 处理opcua-item节点
                    if ("OpcUa-Item".equalsIgnoreCase(node.getString("type"))) {
                        String itemNodeJsonTpl = node.toString();
                        itemNodeJsonTpl = itemNodeJsonTpl.replace("$id_opcua_item", opcuaItemNodeId);
                        itemNodeJsonTpl = itemNodeJsonTpl.replace("$id_opcua_client", clientId);
                        itemNodeJsonTpl = itemNodeJsonTpl.replace("$item_addr", uns.getFields().get(j).getIndex());
                        FieldType ft = uns.getFields().get(j).getType();
                        // 将uns的字段类型转换为node-red的opcua-item节点的数据类型
                        String itemDataType = DataType.transfer(ft);
                        itemNodeJsonTpl = itemNodeJsonTpl.replace("$data_type", itemDataType);
                        itemNodeJsonTpl = itemNodeJsonTpl.replace("\\", "\\\\");
                        JSONObject newItemNode = JSON.parseObject(itemNodeJsonTpl);
                        newItemNode.put("y", baseHeight + 40 * j);
                        itemNodeArr.add(newItemNode);
                    }
                }

            }
        }
        return itemNodeArr;
    }

    private boolean isTemplateNode(JSONObject injectNode) {
        String id = injectNode.getString("id");
        return id.startsWith("$");
    }
}
