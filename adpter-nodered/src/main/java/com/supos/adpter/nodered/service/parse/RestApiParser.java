package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.protocol.RestConfigDTO;
import com.supos.common.enums.IOTProtocol;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("restApiParser")
public class RestApiParser extends ParserApi {

    private String tplGetFile = "/rest-get.json.tpl";

    private String tplPOSTFile = "/rest-post.json.tpl";


    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        Object method = uns.getConfig().get("method");
        if (method == null || "GET".equalsIgnoreCase(method.toString())) {
            return readFromTpl(tplGetFile);
        }
        return readFromTpl(tplPOSTFile);
    }

    @Override
    public void parse(String tpl, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {

        RestConfigDTO restConfig = JSON.parseObject(JSON.toJSONString(uns.getConfig()), RestConfigDTO.class, Feature.TrimStringFieldValue);
        String serverConfigJson = JSON.toJSONString(restConfig.getServer());
        super.createServer(restConfig.getServerName(), IOTProtocol.REST.getName(), serverConfigJson);

        String injectNodeId = IDGenerator.generate();
        String mqttNodeId = IDGenerator.generate();
        String httpRequestNodeId = IDGenerator.generate();
        String catchNodeId = IDGenerator.generate();
        String catchDebugNodeId = IDGenerator.generate();
        String selectModelNodeId = IDGenerator.generate();

        // 替换节点id
        String jsonFlowStr = tpl.replaceAll("\\$id_inject", injectNodeId)
                .replaceAll("\\$id_mqtt", mqttNodeId)
                .replaceAll("\\$id_catch", catchNodeId)
                .replaceAll("\\$id_catch_debug", catchDebugNodeId)
                .replaceAll("\\$id_model_selector", selectModelNodeId)
                .replaceAll("\\$id_http_request", httpRequestNodeId);

        // 替换定时任务，轮询时间间隔
        jsonFlowStr = jsonFlowStr.replace("$repeat", restConfig.getSyncRate().getSeconds() + "");
        // 替换模型数据 json字符串
        // 替换body
        jsonFlowStr = jsonFlowStr.replace("$http_body", restConfig.getBody().replace("\"", "\\\""));
        // 替换url
        jsonFlowStr = jsonFlowStr.replace("$http_url", restConfig.gainFullUrl());

        JSONArray jsonArr = JSON.parseArray(jsonFlowStr);

        int maxHeight = super.getMaxHeight(fullNodes);
        for (int i = 0; i < jsonArr.size(); i++) {
            // 设置节点高度
            jsonArr.getJSONObject(i).put("y", maxHeight + intervalHeight);
            // 添加header
            String nodeType = jsonArr.getJSONObject(i).getString("type");
            if ("supmodel".equals(nodeType)) {
                jsonArr.getJSONObject(i).put("model", uns.getModel());
            }

            if ("http request".equals(nodeType)) {
                JSONArray headerArray = jsonArr.getJSONObject(i).getJSONArray("headers");
                List<JSONObject> newHeaders = buildHeadersJsonObject(restConfig);
                if (!newHeaders.isEmpty()) {
                    headerArray.addAll(newHeaders);
                }
            }
        }
        fullNodes.addAll(jsonArr);
    }

    // {
    //                "keyType": "Content-Type",
    //                "keyValue": "",
    //                "valueType": "application/json",
    //                "valueValue": ""
    // }
    private List<JSONObject> buildHeadersJsonObject(RestConfigDTO restConfig) {
        JSONArray headers = restConfig.getHeaders();
        List<JSONObject> newHeaders = new ArrayList<>();
        if (headers != null) {
            for (int i = 0; i < headers.size(); i++) {
                JSONObject headerJsonObj = headers.getJSONObject(i);
                JSONObject header = new JSONObject();
                header.put("keyType", "other");
                header.put("keyValue", headerJsonObj.getString("key"));
                header.put("valueType", "other");
                header.put("valueValue", headerJsonObj.getString("value"));
                newHeaders.add(header);
            }
        }
        return newHeaders;
    }

}
