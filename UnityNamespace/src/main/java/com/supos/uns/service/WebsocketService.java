package com.supos.uns.service;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.net.URLDecoder;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.supos.common.Constants;
import com.supos.common.adpater.TopicMessageConsumer;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.JsonResult;
import com.supos.common.event.*;
import com.supos.common.sdk.WebsocketSender;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.DataUtils;
import com.supos.common.utils.FileUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.common.utils.UserContext;
import com.supos.i18n.service.I18nExcelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.postgresql.jdbc.TimestampUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WebsocketService implements WebsocketSender {

    private static final ConcurrentHashMap<String, WebsocketService.WsSubscription> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Set<String>> idToSessionsMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Set<String>> topicToSessionsMap = new ConcurrentHashMap<>();
    private final ExecutorService dataPublishExecutor = new ForkJoinPool(1);

    final UnsQueryService unsQueryService;
    final UnsTopologyService unsTopologyService;
    final IUnsDefinitionService definitionService;
    final GlobalExportService globalExportService;
    final UnsExcelService unsExcelService;
    final I18nExcelService i18nExcelService;
    @Autowired
    TopicMessageConsumer topicMessageConsumer;

    public WebsocketService(@Autowired UnsQueryService unsQueryService, @Autowired UnsExcelService unsExcelService,
                               @Autowired UnsTopologyService unsTopologyService,
                               @Autowired IUnsDefinitionService definitionService,
                               @Autowired GlobalExportService globalExportService,
                               @Autowired I18nExcelService i18nExcelService
    ) {
        this.unsQueryService = unsQueryService;
        this.unsExcelService = unsExcelService;
        this.unsTopologyService = unsTopologyService;
        this.definitionService = definitionService;
        this.globalExportService = globalExportService;
        this.i18nExcelService = i18nExcelService;
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
    /**
     * open-api实时值订阅会话
     * MAP : <alias,<sessionId,subValueObj>
     */
    private static final ConcurrentHashMap<String, Map<String, Object>> aliasToSessionsMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, WebSocketSession> topologySessions = new ConcurrentHashMap<>();

    private static final String SEND_PREV = "/send?t=";
    private static final String SEND_BODY = "&body=";

    public ConcurrentHashMap<String, WebsocketService.WsSubscription> getSessions() {
        return sessions;
    }

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), new WebsocketService.WsSubscription(session));
    }

    @Override
    public void sendLatestMsg(WebsocketNotifyEvent event) {
        Long unsId = event.unsId;
        if (unsId != null) {
            Set<String> connectionIds = idToSessionsMap.get(unsId);
            connectionIds = tryGetCitingSubscribe(unsId, connectionIds);
            if (!CollectionUtils.isEmpty(connectionIds)) {
                String topicLastMessage = getTopicLastMessage(unsId);
                TextMessage message = new TextMessage(topicLastMessage);
                for (String connId : connectionIds) {
                    WebsocketService.WsSubscription subscription = sessions.get(connId);
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
            final String topic = event.path;
            Set<String> connectionIds = topicToSessionsMap.get(topic);
            if (!CollectionUtils.isEmpty(connectionIds)) {
                String topicLastMessage = getTopicLastMessage(topic);
                TextMessage message = new TextMessage(topicLastMessage);
                for (String connId : connectionIds) {
                    WebsocketService.WsSubscription subscription = sessions.get(connId);
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

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(100)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        long t0 = System.currentTimeMillis();
        for (CreateTopicDto file : event.topics) {
            Long unsId = file.getId();
//            String topic = file.getTopic();
//            unsTopologyService.removeFromGlobalTopologyData(topic);
            Set<String> connectionIds = idToSessionsMap.remove(unsId);
            if (connectionIds != null) {
                for (String connId : connectionIds) {
                    WebsocketService.WsSubscription subscription = sessions.get(connId);
                    if (subscription != null) {
                        subscription.unsIds.remove(unsId);
                    }
                }
            }
        }
        long t1 = System.currentTimeMillis();
        log.info("删除耗时 : {} ms", t1-t0);
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

    public void handleSessionConnected(WebSocketSession session) {
        log.info(LocaleContextHolder.getLocale().getLanguage());
        UriComponents components = UriComponentsBuilder.fromUri(session.getUri()).build();
        List<String> idStrs = components.getQueryParams().get("id");
        List<String> topics = components.getQueryParams().get("topic");

        final String connectionId = session.getId();


        if (CollectionUtils.isEmpty(idStrs) && CollectionUtils.isEmpty(topics)) {
            Pair<String, String> isImportRequest = importRequest(components);
            if (isImportRequest != null) {
                String file = isImportRequest.getLeft();
                if (isImportRequest.getRight().equals("global")) {
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
                } else if (isImportRequest.getRight().equals("i18n")) {
                    // 国际化导入
                    // uns导入
                    String path = URLDecoder.decode(file, StandardCharsets.UTF_8);
                    File excelFile = new File(FileUtils.getFileRootPath(), path);
                    i18nExcelService.asyncImport(excelFile, runningStatus -> dataPublishExecutor.submit(() -> {
                        String json = null;
                        try {
                            json = JsonUtil.toJson(runningStatus);
                            session.sendMessage(new TextMessage(json));
                        } catch (Throwable e) {
                            log.error("fail to send uploadStatus: " + json, e);
                        }
                    }), true);
                } else if (isImportRequest.getRight().equals("uns")) {
                    // uns导入
                    HttpHeaders httpHeaders = session.getHandshakeHeaders();
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
                    }), true, org.apache.commons.lang3.StringUtils.join(httpHeaders.getAcceptLanguage().stream().map(Locale.LanguageRange::getRange).collect(Collectors.toList()), ','));
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
                String decodeTopic = URLDecoder.decode(topic, StandardCharsets.UTF_8);
                topicToSessionsMap.computeIfAbsent(decodeTopic, k -> Collections.synchronizedSet(new TreeSet<>())).add(connectionId);
                publishMessage(session, decodeTopic);
            }
        }
    }

    private Pair<String, String> importRequest(UriComponents components) {
        String file = components.getQueryParams().getFirst("file");
        // 这是导入请求
        if (file != null) {
            String global = components.getQueryParams().getFirst("global");
            if(StringUtils.hasText(global)){
                // 这是全局导入请求
                return Pair.of(file, "global");
            }

            String i18n = components.getQueryParams().getFirst("i18n");
            if(StringUtils.hasText(i18n)){
                // 这是i18n导入请求
                return Pair.of(file, "i18n");
            }

            return Pair.of(file, "uns");
        }
        return null;
    }

    private static Set<String> getConnectionIds(Long unsId) {
        return idToSessionsMap.computeIfAbsent(unsId, k -> Collections.synchronizedSet(new TreeSet<>()));
    }

    public void handleCmdMsg(String payload, WebSocketSession session) throws IOException {
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
            if (!JSONUtil.isTypeJSONObject(payload)) {
                sendCmdMessage("不是标准的json请求结构", HttpStatus.SC_BAD_REQUEST, session, null);
                return;
            }
            JSONObject rootNode = JSONObject.parseObject(payload);
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
        }
    }

    private void aliasDataPush(WebSocketSession session, String version, JSONObject aliasMap) {
        List<JSONObject> dataArray = new ArrayList<>();
        Map<Long, Map<String, Object>> vqtDataMap = vqtQueryLastData(aliasMap);
        for (String alias : aliasMap.keySet()) {
            JSONObject aliasObj = aliasMap.getJSONObject(alias);
            Map<String, Object> newJson = new HashMap<>();
            Map<String, Object> payload = null;
            CreateTopicDto def = definitionService.getDefinitionByAlias(alias);
            // 先查VQT map
            if (def != null && MapUtils.isNotEmpty(vqtDataMap.get(def.getId()))) {
                Map<String, Object> vqtData = vqtDataMap.get(def.getId());
                vqtData.remove("tag");
                Date t = (Date) vqtData.get(Constants.SYS_FIELD_CREATE_TIME);
                vqtData.put(Constants.SYS_FIELD_CREATE_TIME, t.getTime());
                payload = vqtData;
            }

            if (MapUtils.isEmpty(payload)) {
                String rs = unsQueryService.getLastMsgByAlias(alias, true).getData();
                if (StringUtils.hasText(rs) && !"{}".equals(rs)) {
                    payload = JSONObject.parseObject(rs).getJSONObject("data");
                }
            }
            //pride：如果无消息如果所订查询的文件当前无实时值， 则status=通讯异常（0x80 00 00 00 00 00 00 00），
            if (MapUtils.isEmpty(payload)) {
                com.alibaba.fastjson.JSONObject defaultData =  DataUtils.transEmptyValue(def,false);
                newJson = JSONObject.parseObject(defaultData.toJSONString());
            } else {
                //所有数据的payload
                unsQueryService.standardizingData(alias, payload);
                if (!aliasObj.getBooleanValue("all")) {
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
            }

            JSONObject dataMap = new JSONObject();
            dataMap.put("alias", alias);
            dataMap.put("value", newJson);
            dataArray.add(dataMap);
        }
        valuePushResponse(session, version, dataArray);
    }

    private Map<Long, Map<String, Object>> vqtQueryLastData(JSONObject aliasMap) {
        List<CreateTopicDto> defList = new ArrayList<>();
        for (String alias : aliasMap.keySet()) {
            CreateTopicDto def = definitionService.getDefinitionByAlias(alias);
            if (def != null && StringUtils.hasText(def.getTbFieldName())) {
                defList.add(def);
            }
        }
        Map<Long, Map<String, Object>> tagValuesMap = new HashMap<>();
        List<List<CreateTopicDto>> partition = ListUtil.partition(defList, 1000);
        for (List<CreateTopicDto> topicDtos : partition) {
            Map<String, List<Long>> tableIdsMap = topicDtos.stream().collect(Collectors.groupingBy(CreateTopicDto::getTableName, Collectors.mapping(CreateTopicDto::getId, Collectors.toList())));
            for (String tableName : tableIdsMap.keySet()) {
                BatchQueryLastMsgVqtEvent lastMsgEvent = new BatchQueryLastMsgVqtEvent(this,tableName, tableIdsMap.get(tableName));
                EventBus.publishEvent(lastMsgEvent);
                List<Map<String, Object>> values = lastMsgEvent.getValues();
                Map<Long, Map<String, Object>> tags = values.stream().collect(Collectors.toMap(stringObjectMap -> (Long)stringObjectMap.get("tag"), Function.identity()));
                tagValuesMap.putAll(tags);
            }
        }
        return tagValuesMap;
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
                    WebsocketService.WsSubscription subscription = sessions.get(connId);
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

    public void handleSessionClosed(String connectionId) {
        WsSubscription subscription = sessions.remove(connectionId);
        if (subscription != null) {

            for (Long unsId : subscription.unsIds) {
                Set<String> connectionIds = idToSessionsMap.get(unsId);
                if (connectionIds != null) {
                    connectionIds.remove(connectionId);
                }
            }

            for (String topic : subscription.topics) {
                String decodeTopic = URLDecoder.decode(topic, StandardCharsets.UTF_8);
                Set<String> connectionIds = topicToSessionsMap.get(decodeTopic);
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
}
