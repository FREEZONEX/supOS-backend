package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.protocol.MqttConfigDTO;
import com.supos.common.enums.IOTProtocol;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Iterator;

/**
 * 解析时序模型对应的node-red模版文件
 */
@Service("mqttParser")
public class MqttParser extends ParserApi {

    private String tplFile = "/mqtt.json.tpl";

    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        return readFromTpl(tplFile);
    }

    @Override
    public void parse(String tpl, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {

        MqttConfigDTO mqttConfig = JSON.parseObject(JSON.toJSONString(uns.getConfig()), MqttConfigDTO.class, Feature.TrimStringFieldValue);

        String serverConfigJson = JSON.toJSONString(mqttConfig.getServer());
        String serverId = "85bb67b2dbefe3ba";
        if (!"emqx".equals(mqttConfig.getServer().getHost()) || !"1883".equals(mqttConfig.getServer().getPort())) {
            serverId = super.createServer(mqttConfig.getServerName(), IOTProtocol.MQTT.getName(), serverConfigJson);
        }
        // 只会存在一个mqtt out节点
        JSONObject mqttOutNode = getMqttOut(fullNodes);
        int maxHeight = super.getMaxHeight(fullNodes);
        // 查找server对应的mqtt in节点
        JSONObject mqttInNode = getMqttInByServerId(fullNodes, serverId);
        if (mqttInNode != null) {
            JSONObject supModelNode = handleDynamicNode(uns, mqttOutNode.getString("id"), maxHeight);
            // mqtt in追加输出
            JSONArray wires = mqttInNode.getJSONArray("wires");
            JSONArray wire0 = wires.getJSONArray(0);
            String supModeId = supModelNode.getString("id");
            wire0.add(supModeId);
            fullNodes.add(supModelNode);
            return;
        }

        JSONObject defaultMqttBrokerNode = getMqttBrokerNode(fullNodes,"", "emqx", "1883");
        if (defaultMqttBrokerNode != null && !serverId.equals(defaultMqttBrokerNode.getString("id"))) {
            fullNodes.remove(defaultMqttBrokerNode);
        }

        String mqttInId = IDGenerator.generate();
        String mqttOutId = mqttOutNode == null ? IDGenerator.generate() : mqttOutNode.getString("id");
        // 替换节点id
        String jsonFlowStr = tpl.replaceAll("\\$mqtt_in_id", mqttInId)
                .replaceAll("\\$mqtt_out_id", mqttOutId)
                .replaceAll("\\$id_model_selector", IDGenerator.generate())
                .replaceAll("\\$mqtt_broker_input_id", serverId);
        // 替换模型topic
        jsonFlowStr = jsonFlowStr.replace("$model_topic", uns.getUnsTopic());

        // 替换mqtt in名称
        jsonFlowStr = jsonFlowStr.replace("$mqtt_in_name", mqttConfig.getInputName());
        // 替换mqtt in topic
        jsonFlowStr = jsonFlowStr.replace("$mqtt_topic_input", mqttConfig.getInputTopic());

        jsonFlowStr = jsonFlowStr.replace("$mqtt_broker_input_name", mqttConfig.getServerName());
        jsonFlowStr = jsonFlowStr.replace("$mqtt_broker_input_host", mqttConfig.getServer().getHost());
        jsonFlowStr = jsonFlowStr.replace("$mqtt_broker_input_port", mqttConfig.getServer().getPort());
        if (StringUtils.hasText(mqttConfig.getServer().getUsername())) {
            jsonFlowStr = jsonFlowStr.replace("$mqtt_broker_input_credential_user", mqttConfig.getServer().getUsername());
        } else {
            jsonFlowStr = jsonFlowStr.replace("$mqtt_broker_input_credential_user", "");
        }
        if (StringUtils.hasText(mqttConfig.getServer().getPassword())) {
            jsonFlowStr = jsonFlowStr.replace("$mqtt_broker_input_credential_password", mqttConfig.getServer().getPassword());
        } else {
            jsonFlowStr = jsonFlowStr.replace("$mqtt_broker_input_credential_password", "");
        }

        JSONArray jsonArr = JSON.parseArray(jsonFlowStr);
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
                jsonArr.getJSONObject(i).put("model", uns.getModel());
            }
        }
        // 删除重复的mqtt out节点
        if (mqttOutNode != null) {
            Iterator<JSONObject> iterator = jsonArr.iterator();
            String id0 = mqttOutNode.getString("id");
            while (iterator.hasNext()) {
                JSONObject element = iterator.next();
                String id1 = element.getString("id");
                String type = element.getString("type");
                if ("mqtt out".equals(type) && !id1.equals(id0)) {
                    iterator.remove();
                }
            }
        }
        fullNodes.addAll(jsonArr);
    }

    private JSONObject handleDynamicNode(BatchImportRequestVO.UnsVO uns, String mqttOutId, int baseHeight) {
        String tpl = "{\"id\": \"$id_model_selector\",\"type\": \"supmodel\",\"z\": \"\",\"selectedModel\": \"$model_topic\",\"modelSchema\": \"$schema_json_string\",\"modelMapping\": \"\",\"x\": 550,\"y\": 180,\"wires\": [[\"$mqtt_out_id\"],[]]}";

        tpl = tpl.replace("$id_model_selector", IDGenerator.generate());
        tpl = tpl.replace("$model_topic", uns.getUnsTopic());
        tpl = tpl.replace("$mqtt_out_id", mqttOutId);
        JSONObject supModelNode = JSON.parseObject(tpl);
        supModelNode.put("model", uns.getModel());
        supModelNode.put("y", baseHeight + 30);
        return supModelNode;
    }

    private JSONObject getMqttBrokerNode(JSONArray fullNodes, String brokerName, String host, String port) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if ("mqtt-broker".equals(nodeType)) {
                String name = fullNodes.getJSONObject(i).getString("name");
                if (StringUtils.hasText(brokerName) && brokerName.equals(name)) {
                    return fullNodes.getJSONObject(i);
                }
                String brokerHost = fullNodes.getJSONObject(i).getString("broker");
                String brokerPort = fullNodes.getJSONObject(i).getString("port");
                if (brokerHost.equals(host) && brokerPort.equals(port)) {
                    return fullNodes.getJSONObject(i);
                }
            }
        }
        return null;
    }

    private JSONObject getMqttInByServerId(JSONArray fullNodes, String serverId) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if ("mqtt in".equalsIgnoreCase(nodeType)) {
                String sid = fullNodes.getJSONObject(i).getString("id_server");
                if (serverId.equals(sid)) {
                    return fullNodes.getJSONObject(i);
                }
            }
        }
        return null;
    }

    private JSONObject getSupModel(JSONArray fullNodes) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if ("supmodel".equalsIgnoreCase(nodeType)) {
                return fullNodes.getJSONObject(i);
            }
        }
        return null;
    }

    private JSONObject getMqttOut(JSONArray fullNodes) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if ("mqtt out".equalsIgnoreCase(nodeType)) {
                return fullNodes.getJSONObject(i);
            }
        }
        return null;
    }


}
