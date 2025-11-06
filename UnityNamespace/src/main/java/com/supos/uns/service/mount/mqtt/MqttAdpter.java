package com.supos.uns.service.mount.mqtt;

import com.alibaba.fastjson.JSONObject;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.mount.meta.connection.ConnectionReq;
import com.supos.common.dto.mount.meta.connection.ConnectionResp;
import com.supos.common.enums.mount.MountMetaQueryType;
import com.supos.common.enums.mount.MountModel;
import com.supos.common.enums.mount.MountSourceType;
import com.supos.common.event.EventBus;
import com.supos.common.event.mount.ConnectMountMetaQueryEvent;
import com.supos.common.event.mount.MountSourceOnlineEvent;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.service.mount.MountCoreService;
import com.supos.uns.service.mount.MountUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author sunlifang
 * @version 1.0
 * @description: MQTT适配器
 * @date 2025/9/26 13:36
 */
@Slf4j
public class MqttAdpter {

    private final String clientIdPrefix = "supos_mount_mqtt_subscribe";

    // 用于存储多个MQTT客户端，key为brokerUrl
    private final ConcurrentHashMap<String, MqttClient> mqttClients = new ConcurrentHashMap<>();

    @Getter
    private final ConcurrentHashMap<String, Map<String, String> > topicMaps = new ConcurrentHashMap<>();

    private final CopyOnWriteArraySet<String> subscribeStatusSet = new CopyOnWriteArraySet<>();

    private MountCoreService mountCoreService;

    public MqttAdpter(MountCoreService mountCoreService) {
        this.mountCoreService = mountCoreService;
    }

    /**
     * 查询MQTT在离线状态
     * @param unsMountPo
     */
    public Boolean queryOnline(UnsMountPo unsMountPo) {
        AtomicBoolean success = new AtomicBoolean(true);
        AtomicReference<Boolean> result = new AtomicReference<>();

        Consumer<Boolean> callback = (resp) -> {
            if (resp == null) {
                success.set(false);
            }
            result.set(resp);
        };

        MountSourceOnlineEvent event = new MountSourceOnlineEvent(this, MountSourceType.MQTT, unsMountPo.getSourceAlias(), callback);
        EventBus.publishEvent(event);

        if (!success.get()) {
            throw new RuntimeException("query mqtt online error");
        }
        return result.get();
    }

    /**
     * 获取MQTT连接信息
     * @return
     */
    public ConnectionResp queryMqtt(String sourceAlias) {
        AtomicBoolean success = new AtomicBoolean(true);
        AtomicReference<ConnectionResp> result = new AtomicReference<>();

        ConnectionReq param = new ConnectionReq();
        param.setName(sourceAlias);

        Consumer<Object> callback = (resp) -> {
            if (resp == null) {
                success.set(false);
            }
            result.set((ConnectionResp) resp);
        };

        ConnectMountMetaQueryEvent event = new ConnectMountMetaQueryEvent(this, null, MountMetaQueryType.MQTT_BROKER,
                param, callback);
        EventBus.publishEvent(event);

        if (!success.get()) {
            throw new RuntimeException("query mqtt connect info error");
        }

        return result.get();
    }

    /**
     * 初始化MQTT客户端
     * @param config MQTT broker配置
     */
    public MqttClient initMqttClient(String brokerUrl, JSONObject config) {
        // 如果客户端已存在且连接正常，则直接返回
        String connectName = config.getString("connectName");
        MqttClient client = mqttClients.get(connectName);
        if (client != null && client.isConnected()) {
            return client;
        }

        try {
            String clientId = clientIdPrefix + "-" + UUID.randomUUID().toString();
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(15);
            options.setKeepAliveInterval(20);
            options.setAutomaticReconnect(true);

            String username = config.getString("username");
            if (username != null) {
                options.setUserName(username);
            }

            String password = config.getString("password");
            if (password != null) {
                options.setPassword(password.toCharArray());
            }

            client.connect(options);
            mqttClients.put(connectName, client);
            log.info("mqtt connect success: {}", brokerUrl);
        } catch (Exception e) {
            log.error("mqtt connect error: {}", brokerUrl, e);
        }
        return client;
    }

    /**
     * 订阅MQTT主题
     * @param config 其它配置
     */
    public void subscribeTopic(final JSONObject config) {
        String brokerUrl = String.format("tcp://%s:%s", config.getString("host"), config.getString("port"));
        String topic = "#";
        // 确保MQTT客户端已初始化
        MqttClient client = initMqttClient(brokerUrl, config);

        if (client == null || !client.isConnected()) {
            log.warn("mqtt connect error, can not subscribe: {} @ {}", topic, brokerUrl);
            return;
        }

        try {
            String connectName = config.getString("connectName");
            if (subscribeStatusSet.add(connectName)) {
                // 订阅回调
                client.subscribe(topic, (t, message) -> {
                    // 最新消息存储到内存
                    handleMqttMessage(t, message, connectName);
                });

                log.info("subscribe MQTT success : {} @ {}", topic, brokerUrl);
            }
        } catch (MqttException e) {
            log.error("subscribe MQTT error : {} @ {}", topic, brokerUrl, e);
        }
    }

    /**
     * 处理MQTT消息
     * @param topic MQTT主题
     * @param message MQTT消息
     * @param connectName 关联的挂载信息
     */
    private void handleMqttMessage(String topic, MqttMessage message, String connectName) {
        try {
            // 根据消息内容处理元数据变更
            log.info("receive MQTT message - topic: {}, connectName: {}", topic, connectName);

            String payload = new String( message.getPayload(), StandardCharsets.UTF_8);
            // 缓存最新消息
            topicMaps.computeIfAbsent(connectName, k -> new ConcurrentHashMap<>()).put(topic, payload);
            String alias = MountUtils.alias(MountSourceType.MQTT, connectName, topic);
            CreateTopicDto file = mountCoreService.getDefinitionByAlias(alias);
            if (file == null) {
                List<UnsMountPo> mountPos = mountCoreService.queryMountInfo(MountModel.MQTT_ALL, null,null);
                MqttMountAdpter adapter = mountCoreService.getAdapter(MqttMountAdpter.class);
                if (!mountPos.isEmpty() && adapter != null) {
                    adapter.getMqttMetaChangeManager().handleTopic(mountPos.get(0), connectName);
                }
            }
            mountCoreService.saveTopicPayloadToUns(MountSourceType.MQTT, connectName, topic, payload);
        } catch (Exception e) {
            log.error("handle MQTT message error : {}", topic, e);
        }
    }

    /**
     * 关闭指定的MQTT客户端
     * @param config 连接配置
     */
    public void closeMqttClient(JSONObject config) {
        String connectName = config.getString("connectName");
        MqttClient client = mqttClients.get(connectName);
        if (client != null && client.isConnected()) {
            try {
                client.unsubscribe("#");
                client.disconnect();
                client.close();
                subscribeStatusSet.remove(connectName);
                topicMaps.remove(connectName);
                mqttClients.remove(connectName);
                log.info("MQTT close success: {}", connectName);
            } catch (MqttException e) {
                log.error("MQTT close error: {}", connectName, e);
            }
        }
    }
}
