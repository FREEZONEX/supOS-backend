package com.supos.common.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.enums.GlobalExportModuleEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月21日 14:14
 */
@Slf4j
public class NodeRedUtils {
    private NodeRedUtils() {
    }
    public static String getTag(String nodeId,String nodeRedHost, String nodeRedPort){
        String url;
        if(StringUtils.hasText(nodeRedPort)){
            url = String.format("http://%s:%s/nodered-api/load/tags?nodeId=%s", nodeRedHost, nodeRedPort, nodeId);
        }else {
            url = String.format("%s/nodered-api/load/tags?nodeId=%s", nodeRedHost,nodeId);
        }
        return HttpUtil.get(url);
    }
    public static void saveTags(String nodeId,List<String[]> tags, String nodeRedHost, String nodeRedPort){
        String url;
        if(StringUtils.hasText(nodeRedPort)){
            url = String.format("http://%s:%s/nodered-api/save/tags", nodeRedHost, nodeRedPort);
        }else {
            url = "http://100.100.100.22:33893/nodered-api/save/tags";
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json; charset=UTF-8");
        HttpRequest httpClient = HttpUtil.createRequest(Method.POST, url);
        JSONObject requestBody = new JSONObject();
        requestBody.put("nodeId", nodeId);
        requestBody.put("tags", tags);
        httpClient.addHeaders(headers);
        httpClient.body(requestBody.toJSONString());
        HttpResponse response = httpClient.execute();
    }
    public static String create(GlobalExportModuleEnum globalExportModuleEnum, String flowId, String flowName, String description, JSONArray nodes, String nodeRedHost, String nodeRedPort) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("id", flowId);
        requestBody.put("nodes", nodes == null ? new ArrayList<>(1) : nodes);
        requestBody.put("disabled", false);
        requestBody.put("label", flowName);
        requestBody.put("info", description);
        log.debug(">>>>>>>>>>>>>>>" + globalExportModuleEnum.getCode() + " 创建 nodered 请求:{}", requestBody.toJSONString());
        HttpRequest httpClient;
        String url = null;
        if (StringUtils.hasText(flowId)) {
            if(StringUtils.hasText(nodeRedPort)){
                url = String.format("http://%s:%s/flow/%s", nodeRedHost, nodeRedPort,flowId);
            }else {
                url = String.format("%s/flow/%s", nodeRedHost,flowId);
            }
            httpClient = HttpUtil.createRequest(Method.PUT, url);
        } else {
            if(StringUtils.hasText(nodeRedPort)){
                url = String.format("http://%s:%s/flow", nodeRedHost, nodeRedPort);
            }else {
                url = String.format("%s/flow", nodeRedHost);
            }
            httpClient = HttpUtil.createRequest(Method.POST, url);
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json; charset=UTF-8");
        httpClient.addHeaders(headers);
        httpClient.body(requestBody.toJSONString());
        // 连接超时和读取响应超时 10分钟
        httpClient.timeout(10 * 60 * 1000);
        HttpResponse response = httpClient.execute();
        log.debug(">>>>>>>>>>>>>>>" + globalExportModuleEnum.getCode() + " 创建 nodered 返回结果:{}", response.body());
        return JSON.parseObject(response.body()).getString("id");
    }

    public static String get(String nodeRedHost, String nodeRedPort, String flowId) {
        String url = String.format("http://%s:%s/flow/%s", nodeRedHost, nodeRedPort, flowId);
        log.debug(">>>>>>>>>>>>>>>eventFlow 查询 nodered 请求 :{}", url);
        HttpRequest getClient = HttpUtil.createGet(url);
        HttpResponse response = getClient.execute();
        log.debug(">>>>>>>>>>>>>>>eventFlow 查询 nodered 返回结果:{}", response.body());
        if (response.getStatus() != 200) {
            return null;
        }
        return response.body();
    }
}
