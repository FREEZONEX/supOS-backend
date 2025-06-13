package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.Constants;
import org.springframework.stereotype.Service;

/**
 * 解析关系模型对应的node-red模版文件
 */
@Service("relationalParser")
public class RelationalParser extends ParserApi {

    private String tplGmqttFile = "/relational-gmatt.json.tpl";
    private String tplEmqxFile = "/relational-emqx.json.tpl";

    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        if ("gmqtt".equals(Constants.MQTT_PLUGIN)) {
            return readFromTpl(tplGmqttFile);
        }
        return readFromTpl(tplEmqxFile);
    }

    @Override
    public void parse(String tpl, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {

        int maxHeight = super.getMaxHeight(fullNodes);

        String injectNodeId = IDGenerator.generate();
        String selectModelNodeId = IDGenerator.generate();
        String mqttNodeId = IDGenerator.generate();
        String funcNodeId = IDGenerator.generate();
        // 替换节点id
        String jsonFlowStr = tpl.replaceAll("\\$id_inject", injectNodeId)
                .replaceAll("\\$id_model_selector", selectModelNodeId)
                .replaceAll("\\$id_func", funcNodeId)
                .replaceAll("\\$id_mqtt", mqttNodeId);
        // 替换模型topic
        jsonFlowStr = jsonFlowStr.replace("$model_alias", uns.getAlias());
        // 替换mock数据
        if (Constants.useAliasAsTopic) {
            jsonFlowStr = jsonFlowStr.replace("$alias_path_topic", uns.getAlias());
        } else {
            jsonFlowStr = jsonFlowStr.replace("$alias_path_topic", uns.getUnsTopic());
        }
        jsonFlowStr = jsonFlowStr.replace("$payload", uns.getJsonExample());
        jsonFlowStr = jsonFlowStr.replace("$disabled", "false");

        JSONArray jsonArr = JSON.parseArray(jsonFlowStr);
        // 设置节点高度
        for (int i = 0; i < jsonArr.size(); i++) {
            jsonArr.getJSONObject(i).put("y", maxHeight + intervalHeight);
        }
        fullNodes.addAll(jsonArr);
    }

}
