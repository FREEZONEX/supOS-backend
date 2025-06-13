package com.supos.adpter.kong.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.supos.adpter.kong.listener.KongAdapterCommandRunner;
import com.supos.adpter.kong.vo.MarkRouteRequestVO;
import com.supos.adpter.kong.vo.RoutResponseVO;
import com.supos.adpter.kong.vo.RouteVO;
import com.supos.adpter.kong.vo.ServiceResponseVO;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.protocol.KeyValuePair;
import com.supos.common.utils.I18nUtils;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class KongAdapterService {

    private static final String GET_KONG_ROUTE = "routes";
    private static final String GET_KONG_SERVICE = "services";
    private static final String KONG_PLUGINS = "plugins";

    @Value("${kong.host:kong}")
    private String host;
    @Value("${kong.port:8001}")
    private String port;

    @Resource
    private SystemConfig systemConfig;



    /**
     * 查询kong所有路由， 并标记是否被勾选展示
     * @return
     */
    public List<RouteVO> queryRoutes() {
        String url = String.format("http://%s:%s/%s", host, port, GET_KONG_ROUTE);
        HttpRequest request = HttpUtil.createGet(url);
        HttpResponse response = request.execute();
        List<RouteVO> routes = new ArrayList<>();
        if (response.getStatus() != 200) {
            log.error("request kong failed, response: {}", response.body());
            return routes;
        }
        InternalKongResponseVO internalKongResponse = JSON.parseObject(response.body(), InternalKongResponseVO.class);
        for (InternalKongVO kongVO : internalKongResponse.getData()) {
            if (isMenu(kongVO.getTags())) {
                RouteVO rvo = new RouteVO();
                String menuName = I18nUtils.getMessage(kongVO.getName());
                rvo.setName(kongVO.getName());
                rvo.setShowName(menuName);

                List<KeyValuePair<String>> tags = parseTags(kongVO.getTags());
                Optional<KeyValuePair<String>> showName = tags.stream().filter(t -> t.getKey().startsWith("showName:")).findFirst();
                if (showName.isPresent()) {
                    rvo.setShowName(showName.get().getValue());
                }
                rvo.setTags(parseTags(kongVO.getTags()));
                rvo.setService(queryServiceById(kongVO.getService().getId()));
//                rvo.setService();
                // set menu checked or not
                if (kongVO.getPaths() != null && !kongVO.getPaths().isEmpty()) {
//                    boolean checked = KongAdapterCommandRunner.localMenus.containsKey(kongVO.getName());
//                    rvo.setMenu(new RouteVO.MenuVO(kongVO.getPaths().get(0), checked));
                    rvo.setMenu(new RouteVO.MenuVO(kongVO.getPaths().get(0)));
                }
                routes.add(rvo);
            }
        }
        return routes;
    }

    private List<KeyValuePair<String>> parseTags(List<String> tags) {
        List<KeyValuePair<String>> tagList = new ArrayList<>();
        for (String t : tags) {
            if (t.startsWith("parentName:")) {
                String tag = t.replace("parentName:", "");
                String tagShowName = I18nUtils.getMessage(tag);
                tagList.add(new KeyValuePair(t, tagShowName));
            } else if (t.startsWith("description:")) {
                String tag = t.replace("description:", "");
                String tagShowName = I18nUtils.getMessage(tag);
                tagList.add(new KeyValuePair(t, tagShowName));
            } else if (t.startsWith("homeParentName:")) {
                String tag = t.replace("homeParentName:", "");
                String tagShowName = I18nUtils.getMessage(tag);
                tagList.add(new KeyValuePair(t, tagShowName));
            } else if (t.startsWith("showName:")) {
                String tag = t.replace("showName:", "");
                tagList.add(new KeyValuePair(t, tag));
            } else {
                tagList.add(new KeyValuePair("", t));
            }
        }
        return tagList;
    }

    // Check whether the route is a menu
    public boolean isMenu(List<String> tags) {
        return tags != null && tags.contains("menu");
    }

    /**
     * update menu which is checked
     * @param routes
     */
    public void markMenu(List<MarkRouteRequestVO> routes) throws IOException {
        Map<String, String> newLocalMenus = new HashMap<>();
        routes.stream().forEach(r -> {
            newLocalMenus.put(r.getName(),r.getUrl());
        });
        // file storage
        File file = new File(KongAdapterCommandRunner.LOCAL_MENU_CHECKED_STORAGE_PATH);
        if (!file.exists()) {
            // create new file
            file.createNewFile();
        }
        try (FileWriter fileWriter = new FileWriter(KongAdapterCommandRunner.LOCAL_MENU_CHECKED_STORAGE_PATH)) {
            if (!newLocalMenus.isEmpty()) {
                for (Map.Entry<String, String> entry : newLocalMenus.entrySet()) {
                    fileWriter.write(entry.getKey() + "=" + entry.getValue() + "\n");
                }
            } else {
                // clear file
                fileWriter.write("");
            }
        }
        KongAdapterCommandRunner.localMenus = newLocalMenus;
        log.info("update menu cache success");
    }

    @Data
    static class InternalKongVO {

        private String id;
        private String name;

        private List<String> paths;

        private List<String> tags;

        private ServiceResponseVO service;
    }

    @Data
    static class InternalKongResponseVO {

        private List<InternalKongVO> data;
    }

    private String getCategory(List<String> tags){
        tags.remove("menu");
        return CollectionUtil.getFirst(tags);
    }

    public ServiceResponseVO queryServiceById(String serviceId){
        String url = String.format("http://%s:%s/%s/%s", host, port, GET_KONG_SERVICE,serviceId);
        HttpRequest request = HttpUtil.createGet(url);
        HttpResponse response = request.execute();
        if (response.getStatus() != 200) {
            log.error("request kong service failed, response: {}", response.body());
            return null;
        }
        ServiceResponseVO res = JSON.parseObject(response.body(),ServiceResponseVO.class);
        return res;
    }

    public JSONObject queryServiceJsonById(String serviceId){
        String url = String.format("http://%s:%s/%s/%s", host, port, GET_KONG_SERVICE,serviceId);
        HttpRequest request = HttpUtil.createGet(url);
        HttpResponse response = request.execute();
        if (response.getStatus() != 200) {
            log.error("request kong service failed, response: {}", response.body());
            return null;
        }
        return JSON.parseObject(response.body());
    }

    public ServiceResponseVO createService(String serviceName, String serviceProtocol, String serviceHost, int servicePort) {
        String url = String.format("http://%s:%s/%s", host, port, GET_KONG_SERVICE);

        HttpRequest request = HttpUtil.createPost(url);
        JSONObject body = new JSONObject();
        body.put("name", serviceName);
        body.put("protocol", serviceProtocol);
        body.put("host", serviceHost);
        body.put("port", servicePort);
        body.put("enabled", true);
        request.body(body.toJSONString());
        log.info(">>>>>>>>>>>>kong create service URL：{},params:{}", url, body);
        HttpResponse response = request.execute();
        if (response.getStatus() != 200 && response.getStatus() != 201) {
            log.error("request kong service failed, response: {}", response.body());
            throw new RuntimeException("create service error");
        }
        ServiceResponseVO res = JSON.parseObject(response.body(),ServiceResponseVO.class);
        return res;
    }

    public ServiceResponseVO updateService(String id, JSONObject service) {
        String url = String.format("http://%s:%s/%s/%s", host, port, GET_KONG_SERVICE, id);

        HttpRequest request = HttpRequest.put(url);
        request.body(service.toJSONString());
        HttpResponse response = request.execute();
        if (response.getStatus() != 200 && response.getStatus() != 201) {
            log.error("request kong service failed, response: {}", response.body());
            throw new RuntimeException("update service error");
        }
        ServiceResponseVO res = JSON.parseObject(response.body(),ServiceResponseVO.class);
        return res;
    }

    /**
     * 查询路由
     * @param name
     * @return
     */
    public RoutResponseVO fetchRoute(String name) {
        String url = String.format("http://%s:%s/%s/%s", host, port, GET_KONG_ROUTE, name);
        HttpRequest request = HttpUtil.createGet(url);
        HttpResponse response = request.execute();
        if (response.getStatus() != 200) {
            log.error("kong fetch route failed, response: {}", response.body());
            return null;
        }
        return JSON.parseObject(response.body(),RoutResponseVO.class);
    }

    public List<RoutResponseVO> searchRoute(List<String> tags) {
        String url = String.format("http://%s:%s/%s", host, port, GET_KONG_ROUTE);
        HttpRequest request = HttpUtil.createGet(url);
        Map<String, Object> params = new HashMap<>();
        params.put("tags", StringUtils.join(tags, ","));
        request.form(params);
        HttpResponse response = request.execute();
        if (response.getStatus() != 200) {
            log.error("kong fetch route failed, response: {}", response.body());
            return null;
        }
        JSONObject responseJson = JSON.parseObject(response.body());
        return responseJson.getJSONArray("data").toJavaList(RoutResponseVO.class);
    }

    /**
     * 创建路由
     * @param serviceId 服务ID
     * @param name 路由名称
     * @param path 路由路径
     * @param tags 标签
     * @return
     */
    public RoutResponseVO createRoute(String serviceId, String name, String path, List<String> tags) {
        String url = String.format("http://%s:%s/%s", host, port, GET_KONG_ROUTE);

        HttpRequest request = HttpUtil.createPost(url);
        JSONObject body = new JSONObject();

        JSONArray paths = new JSONArray();
        paths.add(path);
        body.put("paths", paths);

        JSONObject service = new JSONObject();
        service.put("id", serviceId);
        body.put("service", service);

        if (CollectionUtil.isNotEmpty(tags)) {
            JSONArray tagArray = new JSONArray();
            tags.forEach(tag -> {
                tagArray.add(tag);
            });
            body.put("tags", tagArray);
        }

        body.put("name", name);

        request.body(body.toJSONString());

        log.info(">>>>>>>>>>>>kong create route URL：{},params:{}", url, body);
        HttpResponse response = request.execute();

        if (response.getStatus() != 200 && response.getStatus() != 201) {
            log.error("kong create route failed, response: {}", response.body());
            throw new RuntimeException("kong create route failed");
        }

        return JSON.parseObject(response.body(),RoutResponseVO.class);
    }

    public void deleteRoute(String name) {
        String url = String.format("http://%s:%s/%s/%s", host, port, GET_KONG_ROUTE, name);

        log.info(">>>>>>>>>>>>kong delete route URL：{}", url);
        HttpRequest request = HttpRequest.delete(url);
        HttpResponse response = request.execute();
        if (response.getStatus() != 204) {
            log.error("kong delete route failed, response: {}", response.body());
        }
    }

    /**
     * 修改路由
     * @param serviceId
     * @param name
     * @param path
     * @param tags
     * @return
     */
    public RoutResponseVO updateRoute(String serviceId, String name, String path, List<String> tags) {
        String url = String.format("http://%s:%s/%s/%s", host, port, GET_KONG_ROUTE, name);

        HttpRequest request = HttpRequest.put(url);
        JSONObject body = new JSONObject();

        JSONArray paths = new JSONArray();
        paths.add(path);
        body.put("paths", paths);

        JSONObject service = new JSONObject();
        service.put("id", serviceId);
        body.put("service", service);

        if (CollectionUtil.isNotEmpty(tags)) {
            JSONArray tagArray = new JSONArray();
            tags.forEach(tag -> {
                tagArray.add(tag);
            });
            body.put("tags", tagArray);
        }

        body.put("name", name);

        request.body(body.toJSONString());
        log.info(">>>>>>>>>>>>kong update route URL：{},params:{}", url, body);
        HttpResponse response = request.execute();

        if (response.getStatus() != 200 && response.getStatus() != 201) {
            log.error("kong update route failed, response: {}", response.body());
            throw new RuntimeException("kong update route failed");
        }

        return JSON.parseObject(response.body(),RoutResponseVO.class);
    }

    public void addApiKey(String name) {
        String url = String.format("http://%s:%s/%s/%s/%s", host, port, GET_KONG_ROUTE, name, KONG_PLUGINS);

        HttpRequest request = HttpRequest.post(url);

        JSONObject  params = new JSONObject();
        params.put("name", "key-auth");

        JSONObject config = new JSONObject();
        config.put("key_names", Lists.newArrayList("apikey"));
        config.put("key_in_header", true);
        config.put("key_in_query", true);
        config.put("anonymous", null);
        config.put("run_on_preflight", true);
        config.put("hide_credentials", false);
        config.put("key_in_body", false);
        config.put("realm", null);
        params.put("config", config);

        params.put("enabled", true);
        params.put("protocols", Lists.newArrayList("grpc","grpcs","http","https"));
        request.body(params.toJSONString());

        log.info(">>>>>>>>>>>>kong addApiKey URL：{},params:{}", url, params);
        HttpResponse response = request.execute();

        if (response.getStatus() != 200 && response.getStatus() != 201) {
            log.error("kong addApiKey failed, response: {}", response.body());
            throw new RuntimeException("kong addApiKey failed");
        }
        System.out.println(response.body());
    }

//    public ResponseEntity pluginsProxy(String id, JSONObject params){
//        String url = "http://kong:8001/" + KONG_PLUGINS + "/" + id;
//        log.info(">>>>>>>>>>>>>pluginsProxy url:{},id:{},params:{}",url,id,params);
//        JSONObject req = new JSONObject();
//        req.put("enabled",params.getBoolean("enabled"));
//        HttpResponse response = HttpRequest.patch(url)
//                .body(req.toJSONString())                                         // 设置请求体
//                .timeout(5000)                                             // 设置超时时间
//                .execute();
//        log.info(">>>>>>>>>>>>>pluginsProxy response:{}",response.body());
//        //如果auth check插件修改启用状态，同步修改环境变量配置
//        if (200 == response.getStatus() && Constants.AUTH_CHECK_KONG_PLUGIN_ID.equals(id)){
//            systemConfig.setAuthEnable(params.getBoolean("enabled"));
//        }
//        return ResponseEntity.status(response.getStatus()).body(response.body());
//    }

//    @PostConstruct
//    public void init() {
//        if (!RuntimeUtil.isLocalRuntime()) {
//            ThreadUtil.execute(() -> {
//                String url = "http://kong:8001/" + KONG_PLUGINS + "/" + Constants.AUTH_CHECK_KONG_PLUGIN_ID;
//                String body = HttpUtil.get(url);
//                if (StringUtils.isNotBlank(body)){
//                    boolean authEnable = JSON.parseObject(body).getBoolean("enabled");
//                    systemConfig.setAuthEnable(authEnable);
//                    log.info(">>>>>>>>>>>>>authEnable set success :{}",authEnable);
//                }
//            });
//        }
//    }


    public static void main(String[] args) {
        String url = String.format("http://%s:%s/%s/%s", "192.168.235.123", "8001", GET_KONG_ROUTE, "e3200881-57f9-49cf-9387-2a9c11dc1a7e");
        HttpRequest request = HttpRequest.delete(url);
        HttpResponse response = request.execute();
        if (response.getStatus() != 200) {
            log.error("request kong service failed, response: {}", response.body());
        }
    }
}
