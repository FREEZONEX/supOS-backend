package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.protocol.ICMPConfigDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 定制化协议，ping目标服务器
 */
@Service("icmpParser")
public class ICMPParser extends ParserApi {

    private String tplFile = "/icmp.json.tpl";

    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        return readFromTpl(tplFile);
    }

    @Override
    public void parse(String tpl, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {

        ICMPConfigDTO icmpConfig = JSON.parseObject(JSON.toJSONString(uns.getConfig()), ICMPConfigDTO.class, Feature.TrimStringFieldValue);
        JSONObject defaultMqttBrokerNode = getMqttBrokerNode(fullNodes,"", "emqx", "1883");
        if (defaultMqttBrokerNode != null) {
            fullNodes.remove(defaultMqttBrokerNode);
        }

        int maxHeight = super.getMaxHeight(fullNodes);

        String selectModelNodeId = IDGenerator.generate();
        String mqttNodeId = IDGenerator.generate();
        String pingNodeId = IDGenerator.generate();
        String injectId = IDGenerator.generate();

        String jsonFlowStr = tpl;
        jsonFlowStr = jsonFlowStr.replaceAll("\\$id_model_selector", selectModelNodeId)
                .replaceAll("\\$id_inject", injectId)
                .replaceAll("\\$id_mqtt_out", mqttNodeId)
                .replaceAll("\\$id_ui_ping", pingNodeId);
        jsonFlowStr = jsonFlowStr.replace("$poll_rate", icmpConfig.getInterval() + "");
        jsonFlowStr = jsonFlowStr.replace("$ping_ip", icmpConfig.getServer().getHost());
        jsonFlowStr = jsonFlowStr.replace("$ping_timeout", icmpConfig.getRetry() * icmpConfig.getTimeout() + "");
        jsonFlowStr = jsonFlowStr.replace("$model_topic", uns.getUnsTopic());

        JSONArray jsonArr = JSON.parseArray(jsonFlowStr);

        // 设置整体流程高度
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
        fullNodes.addAll(jsonArr);
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

}
