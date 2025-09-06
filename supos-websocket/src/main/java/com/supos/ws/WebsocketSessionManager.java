package com.supos.ws;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ws session管理
 */
@Component
@Slf4j
public class WebsocketSessionManager {

    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存放topic和session, 多对多关系
    private static final ConcurrentHashMap<String, Set<String>> topicSessions = new ConcurrentHashMap<>();

    private static final String TOPIC_WILDCARD = ".*";

    public void put(String sessionId, WebSocketSession session) {
        sessions.put(sessionId, session);
    }

    public int getSize() {
        return sessions.size();
    }

    /**
     * 缓存ws client， 绑定topic和sessionId的关系
     * @param topic
     * @param sessionId
     */
    public boolean binding(String topic, String sessionId) {
        if (!sessions.containsKey(sessionId)) {
            log.error("ws: 当前websocket session缓存中不存在, sessionId={}", sessionId);
            return false;
        }
        Set<String> sessionIds = topicSessions.getOrDefault(topic, new HashSet<>());
        sessionIds.add(sessionId);
        topicSessions.put(topic, sessionIds);
        return true;
    }

    /**
     * 发送普通消息给所有的session，需要排除已经被订阅的session
     */
    public synchronized void sendMessageBroadcastSync(String msg, int delayMS) {
        // 需要排除已经被topic订阅的session列表
        Set<String> subscribedSessions = topicSessions.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (!subscribedSessions.contains(entry.getKey())) {
                try {
                    entry.getValue().sendMessage(new TextMessage(msg));
                } catch (IOException e) {
                    log.error("ws: 消息发送失败, msg={}", msg, e);
                }
            }
        }
        try {
            Thread.sleep(delayMS);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    /**
     * 根据topic发送消息给订阅者, 支持.*通配符(仅支持abc.*)
     * @param topic
     * @param jsonMsg
     */
    public void sendMessageByTopic(String topic, String jsonMsg) {
        Set<String> sessionIds = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : topicSessions.entrySet()) {
            // 解析topic中的通配符
            if (entry.getKey().endsWith(TOPIC_WILDCARD)) {
                String prefix = entry.getKey().replace(TOPIC_WILDCARD, "");
                if (topic.startsWith(prefix)) {
                    sessionIds.addAll(entry.getValue());
                }
            } else if(entry.getKey().equals(topic)) {
                sessionIds.addAll(entry.getValue());
            }
        }

        if (sessionIds.isEmpty()) {
            log.warn("ws: 该topic={}不存在订阅者", topic);
            return;
        }

        for (String sessionId : sessionIds) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null) {
                try {
                    session.sendMessage(new TextMessage(jsonMsg));
                } catch (IOException e) {
                    log.error("ws: 消息发送失败, topic={}", topic, e);
                }
            }
        }
    }

    /**
     * 根据sessionId删除对应的订阅
     * @param sessionId
     */
    public void removeBySessionId(String sessionId) {
        sessions.remove(sessionId);
        log.info("ws: 移除websocket会话 sessionId={}", sessionId);
        Set<String> unUsedTopics = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : topicSessions.entrySet()) {
            // 移除只有一个当前订阅者的topic
            if (entry.getValue().contains(sessionId) && entry.getValue().size() == 1) {
                unUsedTopics.add(entry.getKey());
            }
        }
        unUsedTopics.forEach(t -> {
            topicSessions.remove(t);
            log.info("ws: topic={} 订阅被移除", t);
        });
    }
}
