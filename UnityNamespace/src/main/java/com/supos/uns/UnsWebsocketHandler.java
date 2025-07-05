package com.supos.uns;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.net.URLDecoder;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supos.common.Constants;
import com.supos.common.adpater.TopicMessageConsumer;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.JsonResult;
import com.supos.common.dto.SimpleUnsInstance;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.event.TopicMessageEvent;
import com.supos.common.event.UnsTopologyChangeEvent;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.DataUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.service.GlobalExportService;
import com.supos.uns.service.UnsExcelService;
import com.supos.uns.service.UnsQueryService;
import com.supos.uns.service.UnsTopologyService;
import com.supos.uns.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UnsWebsocketHandler implements WebSocketHandler {

    private static final ConcurrentHashMap<String, WsSubscription> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Set<String>> idToSessionsMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Set<String>> topicToSessionsMap = new ConcurrentHashMap<>();

    /**
     * open-api实时值订阅会话
     * MAP : <alias,<sessionId,subValueObj>
     */
    private static final ConcurrentHashMap<String, Map<String, Object>> aliasToSessionsMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, WebSocketSession> topologySessions = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    final UnsQueryService unsQueryService;
    final UnsExcelService unsExcelService;
    final UnsTopologyService unsTopologyService;
    final IUnsDefinitionService definitionService;
    final GlobalExportService globalExportService;
    private final ExecutorService dataPublishExecutor = new ForkJoinPool(1);

    public UnsWebsocketHandler(@Autowired UnsQueryService unsQueryService, @Autowired UnsExcelService unsExcelService,
                               @Autowired UnsTopologyService unsTopologyService,
                               @Autowired IUnsDefinitionService definitionService,
                               @Autowired GlobalExportService globalExportService
    ) {
        this.unsQueryService = unsQueryService;
        this.unsExcelService = unsExcelService;
        this.unsTopologyService = unsTopologyService;
        this.definitionService = definitionService;
        this.globalExportService = globalExportService;
    }

    private static class WsSubscription {
        final WebSocketSession conn;
        final ConcurrentHashSet<Long> unsIds = new ConcurrentHashSet<>();
        final ConcurrentHashSet<String> topics = new ConcurrentHashSet<>();
        final ConcurrentHashSet<String> aliasSet = new ConcurrentHashSet<>();

        WsSubscription(WebSocketSession conn) {
            this.conn = conn;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        final String connectionId = session.getId();
        synchronized (UnsWebsocketHandler.class) {
            if (sessions.size() > Constants.WS_SESSION_LIMIT) {
                try {
                    session.sendMessage(new TextMessage("session reached its maximum capacity " + Constants.WS_SESSION_LIMIT));
                    session.close();
                } catch (IOException e) {
                    //
                }
                log.error("ws会话超过系统限制（{}），当前会话关闭", Constants.WS_SESSION_LIMIT);
                return;
            }
            sessions.put(connectionId, new WsSubscription(session));
        }

        log.info("===> ws sessionSize={}", sessions.size());

        UriComponents components = UriComponentsBuilder.fromUri(session.getUri()).build();
        List<String> idStrs = components.getQueryParams().get("id");
        List<String> topics = components.getQueryParams().get("topic");
        log.debug("WebSocket open: {}", session);
        if (CollectionUtils.isEmpty(idStrs) && CollectionUtils.isEmpty(topics)) {
            String file = components.getQueryParams().getFirst("file");
            if (file != null) {
                String global = components.getQueryParams().getFirst("global");
                if(StringUtils.hasText(global)){
                    // 全局导入
                    String path = URLDecoder.decode(file, StandardCharsets.UTF_8);
                    File zipFile = new File(FileUtils.getFileRootPath(), path);
                    globalExportService.asyncImport(session,zipFile, runningStatus -> dataPublishExecutor.submit(() -> {
                        String json = null;
                        try {
                            json = JsonUtil.toJson(runningStatus);
                            session.sendMessage(new TextMessage(json));
                        } catch (IOException e) {
                            log.error("global import process data fail to send uploadStatus: " + json, e);
                        }
                    }), true);
                }else{
                    // uns导入
                    String path = URLDecoder.decode(file, StandardCharsets.UTF_8);
                    File excelFile = new File(FileUtils.getFileRootPath(), path);
                    unsExcelService.asyncImport(excelFile, runningStatus -> dataPublishExecutor.submit(() -> {
                        String json = null;
                        try {
                            json = JsonUtil.toJson(runningStatus);
                            session.sendMessage(new TextMessage(json));
                        } catch (IOException e) {
                            log.error("fail to send uploadStatus: " + json, e);
                        }
                    }), true);
                }
            }
            String globalTopology = components.getQueryParams().getFirst("globalTopology");
            if (globalTopology != null) {
                topologySessions.put(connectionId, session);
                log.debug("topology: {}", connectionId);
                publishTopologyMessage(session);
            }
            return;
        }
        if (!CollectionUtils.isEmpty(idStrs)) {
            List<Long> ids = idStrs.stream().map(Long::parseLong).toList();
            WsSubscription subscription = sessions.computeIfAbsent(connectionId, k -> new WsSubscription(session));
            log.debug("subscribe: {} topic={}", connectionId, idStrs);
            subscription.unsIds.addAll(ids);
            for (Long id : ids) {
                getConnectionIds(id).add(connectionId);
                publishMessage(session, id);
            }
        }
        if (!CollectionUtils.isEmpty(topics)) {
            WsSubscription subscription = sessions.computeIfAbsent(connectionId, k -> new WsSubscription(session));
            log.debug("subscribe: {} topic={}", connectionId, idStrs);
            subscription.topics.addAll(topics);
            for (String topic : topics) {
                topicToSessionsMap.computeIfAbsent(topic, k -> Collections.synchronizedSet(new TreeSet<>())).add(connectionId);
                publishMessage(session, topic);
            }
        }
    }

    private String getTopicLastMessage(Long id) {
        JsonResult<String> rs = unsQueryService.getLastMsg(id, true);
        String str = rs.getData();
        return str != null ? str : "{}";
    }

    private String getTopicLastMessage(String topic) {
        JsonResult<String> rs = unsQueryService.getLastMsgByPath(topic, true);
        return rs.getData();
    }

    private String getTopicLastMessageByAlias(String alias) {
        JsonResult<String> rs = unsQueryService.getLastMsgByAlias(alias, true);
        return rs.getData();
    }

    private void publishMessage(WebSocketSession session, Long id) {
        String msg = getTopicLastMessage(id);
        try {
            session.sendMessage(new TextMessage(msg));
        } catch (IOException e) {
            log.error("fail to sendWs: id={}, session={}", id, session);
        }
    }

    private void publishMessage(WebSocketSession session, String topic) {
        String msg = getTopicLastMessage(topic);
        if (msg != null) {
            try {
                session.sendMessage(new TextMessage(msg));
            } catch (IOException e) {
                log.error("fail to sendWs: topic={}, session={}", topic, session);
            }
        }
    }

    private void publishTopologyMessage(WebSocketSession session) {
        JsonResult<String> rs = unsTopologyService.getLastMsg();
        String str = rs.getData();
        str = str != null ? str : "{}";
        try {
            session.sendMessage(new TextMessage(str));
        } catch (IOException e) {
            log.error("ws fail to sendTopology: session={}", session);
        }
    }

    private static final String SEND_PREV = "/send?t=";
    private static final String SEND_BODY = "&body=";

    @Autowired
    TopicMessageConsumer topicMessageConsumer;

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            if (!(message instanceof TextMessage)) {
                return;
            }
            TextMessage textMessage = (TextMessage) message;

            String payload = textMessage.getPayload();
            log.trace("WebSocket handleMessage[{}] : {}", session.getId(), payload);

            //heartbeat
            if ("ping".equals(payload)) {
                session.sendMessage(new TextMessage("pong"));
                return;
            }

            if (payload.startsWith(SEND_PREV)) {
                int bodyIndex = payload.indexOf(SEND_BODY, SEND_PREV.length());
                if (bodyIndex > 0) {
                    String alias = payload.substring(SEND_PREV.length(), bodyIndex);
                    String body = payload.substring(bodyIndex + SEND_BODY.length());
                    log.debug("ws onMessage: {}, payload={}", alias, body);
                    if (StringUtils.hasText(alias) && StringUtils.hasText(body)) {
                        topicMessageConsumer.onMessageByAlias(alias, body);
                    }
                }
                return;
            }
            if (payload.indexOf("cmd") > 0) {
                if (!JSONUtil.isTypeJSONObject(textMessage.getPayload())) {
                    sendCmdMessage("不是标准的json请求结构", HttpStatus.SC_BAD_REQUEST, session, null);
                    return;
                }
                JSONObject rootNode = JSONObject.parseObject(textMessage.getPayload());
                JSONObject headNode = rootNode.getJSONObject("head");
                JSONObject dataNode = rootNode.getJSONObject("data");
                if (headNode == null || !headNode.containsKey("cmd")) {
                    sendCmdMessage("head节点不存在或cmd指令为空", HttpStatus.SC_BAD_REQUEST, session, rootNode);
                    return;
                }
                if (dataNode == null) {
                    sendCmdMessage("data节点不存在", HttpStatus.SC_BAD_REQUEST, session, rootNode);
                    return;
                }
                if (headNode.getIntValue("cmd") == Constants.CMD_SUB) {
                    //参数校验未通过
                    if (Objects.isNull(dataNode.get("sub_real_value"))) {
                        sendCmdMessage("sub_real_value参数不存在", HttpStatus.SC_BAD_REQUEST, session, headNode);
                        return;
                    }
                    //别名集合
                    JSONObject aliasMap = dataNode.getJSONObject("sub_real_value");
                    //订阅响应
                    sendCmdMessage("ok", HttpStatus.SC_OK, session, headNode);
                    //实时值推送
                    aliasDataPush(session, headNode.getString("version"), aliasMap);
                    //sessionMap订阅
                    WsSubscription subscription = sessions.computeIfAbsent(session.getId(), k -> new WsSubscription(session));
                    aliasMap.keySet().forEach(alias -> {
                        subscription.aliasSet.add(alias);
                        log.debug("subscribe: {} alias={}", session.getId(), alias);
                        aliasToSessionsMap.computeIfAbsent(alias, k -> Collections.synchronizedSortedMap(new TreeMap<>())).put(session.getId(), aliasMap.get(alias));
                    });
                }
                return;
            }
        } catch (Exception e) {
            log.error(">>>>>>>>>>>>handleMessage IOException", e);
        }
    }

    private void sendCmdMessage(String msg, int scBadRequest, WebSocketSession session, JSONObject headNode) throws IOException {
        JSONObject dataMap = new JSONObject();
        dataMap.put("cmd", Constants.CMD_SUB);
        dataMap.put("msg", msg);
        dataMap.put("status", scBadRequest);
        if (headNode != null) {
            session.sendMessage(new TextMessage(aliasSubResponse(headNode.getString("version"), Constants.CMD_SUB_RES, dataMap)));
        } else {
            session.sendMessage(new TextMessage(msg));
        }

    }

    void uncaughtException(Thread t, Throwable e) {
        log.error("发送Ws数据失败: " + t.getName(), e);
    }

    private static Set<String> getConnectionIds(Long unsId) {
        return idToSessionsMap.computeIfAbsent(unsId, k -> Collections.synchronizedSet(new TreeSet<>()));
    }


    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(100)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        for (SimpleUnsInstance file : event.topics.values()) {
            Long unsId = file.getId();
            String topic = file.getTopic();
            unsTopologyService.removeFromGlobalTopologyData(topic);
            Set<String> connectionIds = idToSessionsMap.remove(unsId);
            if (connectionIds != null) {
                for (String connId : connectionIds) {
                    WsSubscription subscription = sessions.get(connId);
                    if (subscription != null) {
                        subscription.unsIds.remove(unsId);
                    }
                }
            }
        }
    }

    @EventListener(classes = TopicMessageEvent.class)
    @Order(1000)
    void onTopicMessageEvent(TopicMessageEvent event) {
        final Long unsId = event.unsId;
        if (unsId != null) {
            Set<String> connectionIds = idToSessionsMap.get(unsId);
            connectionIds = tryGetCitingSubscribe(unsId, connectionIds);
            if (!CollectionUtils.isEmpty(connectionIds)) {
                String topicLastMessage = getTopicLastMessage(unsId);
                TextMessage message = new TextMessage(topicLastMessage);
                for (String connId : connectionIds) {
                    WsSubscription subscription = sessions.get(connId);
                    try {
                        subscription.conn.sendMessage(message);
                    } catch (IOException e) {
                        log.error("fail to sendMessage to[{}], unsId={}", connId, unsId);
                    }
                }
            } else {
                log.trace("unsId:{}, connectionIds={}", unsId, connectionIds);
            }

            //实时值订阅推送
            CreateTopicDto def = definitionService.getDefinitionById(unsId);
            if (def != null && def.getPathType() == Constants.PATH_TYPE_FILE) {
                String alias = def.getAlias();
                Set<String> aliasList = new HashSet<>();
                aliasList.add(alias);
                //查询引用的Uns列表
                Set<Long> cited = def.getCited();
                Set<String> refersAlias = cited.stream().map(id -> {
                    CreateTopicDto createTopicDto = definitionService.getDefinitionById(id);
                    return createTopicDto.getAlias();
                }).collect(Collectors.toSet());
                aliasList.addAll(refersAlias);
                for (String a : aliasList) {
                    //逐个推送
                    sendOnMessageByAlias(a);
                }
            }
        } else {
            final String topic = event.topic;
            Set<String> connectionIds = topicToSessionsMap.get(topic);
            if (!CollectionUtils.isEmpty(connectionIds)) {
                String topicLastMessage = getTopicLastMessage(topic);
                TextMessage message = new TextMessage(topicLastMessage);
                for (String connId : connectionIds) {
                    WsSubscription subscription = sessions.get(connId);
                    try {
                        subscription.conn.sendMessage(message);
                    } catch (IOException e) {
                        log.error("fail to sendMessage to[{}], topic={}", connId, topic);
                    }
                }
            } else {
                log.trace("topic:{}, connectionIds={}", topic, connectionIds);
            }
        }
    }

    private Set<String> tryGetCitingSubscribe(Long unsId, Set<String> connectionIds) {
        CreateTopicDto def = definitionService.getDefinitionById(unsId);
        Set<Long> cited;
        if (def != null && def.getDataType() != Constants.CITING_TYPE && !CollectionUtils.isEmpty(cited = def.getCited())) {
            if (connectionIds == null) {
                connectionIds = new HashSet<>();
            } else {
                HashSet<String> mergeIds = new HashSet<>(connectionIds.size() + 64);
                mergeIds.addAll(connectionIds);
                connectionIds = mergeIds;
            }
            for (Long ref : cited) {
                Set<String> citingIds = idToSessionsMap.get(ref);
                if (citingIds != null) {
                    connectionIds.addAll(citingIds);
                }
            }
        }
        return connectionIds;
    }

    @EventListener(classes = UnsTopologyChangeEvent.class)
    void onTopologyChangeEvent(UnsTopologyChangeEvent event) {
        if (!topologySessions.isEmpty()) {
            JsonResult<String> rs = unsTopologyService.getLastMsg();
            String str = rs.getData();
            str = str != null ? str : "{}";
            TextMessage msg = new TextMessage(str);
            for (WebSocketSession session : topologySessions.values()) {
                try {
                    session.sendMessage(msg);
                } catch (IOException e) {
                    log.error("fail to sendWs: session={}", session);
                }
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        if (exception != null && exception.getClass() != java.io.EOFException.class) {
            log.error("WebSocket handleTransportError[{}]", session.getId(), exception);
        }
        try {
            session.close();
        } catch (IOException e) {
            //
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            session.close();
        } catch (IOException e) {
            //
        }
        final String connectionId = session.getId();
        log.debug("ws ConnectionClosed: {}, status is {}", connectionId, status.getReason());
        WsSubscription subscription = sessions.remove(connectionId);
        if (subscription != null) {

            for (Long unsId : subscription.unsIds) {
                Set<String> connectionIds = idToSessionsMap.get(unsId);
                if (connectionIds != null) {
                    connectionIds.remove(connectionId);
                }
            }

            for (String topic : subscription.topics) {
                Set<String> connectionIds = topicToSessionsMap.get(topic);
                if (connectionIds != null) {
                    connectionIds.remove(connectionId);
                }
            }

            for (String alias : subscription.aliasSet) {
                Set<String> connectionIds = aliasToSessionsMap.get(alias).keySet();
                if (connectionIds != null) {
                    connectionIds.remove(connectionId);
                }
            }
        }
        WebSocketSession conn = topologySessions.remove(connectionId);
        if (conn != null) {
            try {
                conn.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false; // 不支持部分消息
    }

    private String aliasSubResponse(String version, Integer cmd, JSONObject dataMap) {
        JSONObject resultJson = new JSONObject();
        JSONObject headMap = new JSONObject();
        headMap.put("version", version);
        headMap.put("cmd", cmd);
        if (3 == cmd) {
            JSONArray dataArray = new JSONArray();
            dataArray.add(dataMap);
            resultJson.put("head", headMap);
            resultJson.put("data", dataArray);//用数组返回
        } else {
            resultJson.put("head", headMap);
            resultJson.put("data", dataMap);
        }
        return resultJson.toJSONString();
    }

    /**
     * 分批发送实时值订阅的消息
     *
     * @param session   ws 绘画
     * @param version   版本
     * @param dataArray 数据集合
     */
    private void valuePushResponse(WebSocketSession session, String version, List<JSONObject> dataArray) {
        if (CollectionUtils.isEmpty(dataArray)) {
            return;
        }
        List<List<JSONObject>> batchDataArray = ListUtil.partition(dataArray, Constants.VALUE_PUSH_BATCH_SIZE);
        for (List<JSONObject> objects : batchDataArray) {
            JSONArray array = new JSONArray(objects);
            JSONObject resultJson = new JSONObject();
            JSONObject headMap = new JSONObject();
            headMap.put("version", version);
            headMap.put("cmd", Constants.CMD_VAL_PUSH);
            resultJson.put("head", headMap);
            resultJson.put("data", array);//用数组返回
            String response = resultJson.toJSONString();
            try {
                session.sendMessage(new TextMessage(response));
            } catch (IOException e) {
                log.error("fail to sendWs: session={}", session);
            }
        }
    }

    private void aliasDataPush(WebSocketSession session, String version, JSONObject aliasMap) {
        List<JSONObject> dataArray = new ArrayList<>();
        for (String alias : aliasMap.keySet()) {
            JSONObject aliasObj = aliasMap.getJSONObject(alias);
            String rs = unsQueryService.getLastMsgByAlias(alias, true).getData();
            boolean all = aliasObj.getBooleanValue("all");
            JSONObject jsonObject = JSONObject.parseObject(rs);
            JSONObject newJson = new JSONObject();
            if (jsonObject != null) {
                //所有数据的payload
                JSONObject payload = jsonObject.getJSONObject("data");
                unsQueryService.standardizingData(alias, payload);
                if (!all) {
                    //部分值
                    List<String> partValues = aliasObj.getList("part_value", String.class);
                    Set<String> subscribeTags = new HashSet<>(partValues);
                    //匹配part_value
                    for (Map.Entry<String, Object> en : payload.entrySet()) {
                        String key = en.getKey();
                        if (subscribeTags.contains(key)) {
                            Object value = en.getValue();
                            newJson.put(key, value);
                        }
                    }
                } else {
                    newJson = payload;
                }
            } else {
                CreateTopicDto uns = definitionService.getDefinitionByAlias(alias);
                com.alibaba.fastjson.JSONObject defaultData =  DataUtils.transEmptyValue(uns,false);
                newJson = JSONObject.parseObject(defaultData.toJSONString());
            }
            JSONObject dataMap = new JSONObject();
            dataMap.put("alias", alias);
            dataMap.put("value", newJson);
            dataArray.add(dataMap);
        }
        valuePushResponse(session, version, dataArray);
    }

    private void sendOnMessageByAlias(String alias) {
        //alias,<sessionId，subValueObj>    收到写值变动时，推送websocket client
        Map<String, Object> connectionAliasMap = aliasToSessionsMap.get(alias);
        if (!MapUtils.isEmpty(connectionAliasMap)) {
            String lastMsg = getTopicLastMessageByAlias(alias);
            if (StringUtils.hasText(lastMsg)) {
                for (String connId : connectionAliasMap.keySet()) {
                    TextMessage message = null;
                    Map<String, Object> subValue = (Map<String, Object>) connectionAliasMap.get(connId);
                    Object partOrAll = subValue.get("all");//判断是全量订阅还是部分值订阅
                    if (Boolean.TRUE.equals(partOrAll)) {
                        JSONObject originalData = JSON.parseObject(lastMsg).getJSONObject("data");
                        unsQueryService.standardizingData(alias, originalData);
                        //组装成cmd的格式
                        JSONObject data = new JSONObject();
                        data.put("alias", alias);
                        data.put("value", originalData);
                        String cmdMessage = aliasSubResponse("1.0.0", Constants.CMD_VAL_PUSH, data);
                        message = new TextMessage(cmdMessage);
                    } else {
                        List<String> partValues = (List<String>) subValue.get("part_value");
                        if (CollectionUtils.isEmpty(partValues)) {
                            log.warn("websocket sub_real_value 实时值订阅未推送，part_value为空，alias:{}", alias);
                            continue;
                        }
                        Set<String> subscribeTags = new HashSet<>(partValues);
                        //原始数据
                        JSONObject originalData = JSON.parseObject(lastMsg).getJSONObject("data");
                        unsQueryService.standardizingData(alias, originalData);
                        JSONObject partData = new JSONObject();
                        //匹配part_value
                        for (Map.Entry<String, Object> en : originalData.entrySet()) {
                            String key = en.getKey();
                            if (subscribeTags.contains(key)) {
                                Object value = en.getValue();
                                partData.put(key, value);
                            }
                        }
                        JSONObject data = new JSONObject();
                        data.put("alias", alias);
                        data.put("value", partData);
                        String cmdMessage = aliasSubResponse("1.0.0", Constants.CMD_VAL_PUSH, data);
                        message = new TextMessage(cmdMessage);
                    }
                    WsSubscription subscription = sessions.get(connId);
                    try {

                        subscription.conn.sendMessage(message);
                    } catch (IOException e) {
                        log.error("fail to sendMessage to[{}], topic={}", connId, alias);
                    }
                }
            }
        } else {
            log.trace("alias:{}", alias);
        }
    }
}
