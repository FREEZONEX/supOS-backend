package com.supos.adapter.mqtt.adapter;

import cn.hutool.core.collection.CollectionUtil;
import com.supos.adapter.mqtt.config.MQTTConfig;
import com.supos.adapter.mqtt.dto.ConnectionLossRecord;
import com.supos.adapter.mqtt.service.MQTTPublisher;
import com.supos.adapter.mqtt.service.MessageConsumer;
import com.supos.common.dto.TopologyLog;
import com.supos.common.utils.I18nUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class MQTTConsumerAdapter implements MqttCallback, MQTTPublisher {

    private final MessageConsumer messageConsumer;

    private final String clientId;
    private MqttClient mqttClient;
    public final MemoryPersistence memoryPersistence = new MemoryPersistence();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        AtomicInteger threadNum = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "mqttRetry-" + threadNum.incrementAndGet());
        }
    });
    private final AtomicLong arrivedSize = new AtomicLong();

    private final Set<String> subscribeTopics = new ConcurrentSkipListSet<>();

    public Set<String> getSubscribeTopics() {
        return subscribeTopics;
    }

    public MQTTConsumerAdapter(@Autowired MQTTConfig config, @Autowired MessageConsumer consumer) {
        this.messageConsumer = consumer;
        clientId = config.getClientIdPrefix() + ":" + UUID.randomUUID();
        MqttClient client;
        try {
            // 创建 MQTT 客户端
            client = new MqttClient(config.getBroker(), clientId, memoryPersistence);
            // 设置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(15);
            options.setKeepAliveInterval(20);
            options.setAutomaticReconnect(true);
            // 连接到 EMQX 服务器
            client.connect(options);
            // 设置消息回调
            client.setCallback(this);
        } catch (Exception ex) {
            TopologyLog.log(TopologyLog.Node.PULL_MQTT, TopologyLog.EventCode.ERROR, I18nUtils.getMessage("uns.topology.mqtt.init"));
            log.error("Fail to init MqttClient:" + config.getBroker(), ex);
            System.exit(1);// 初始化失败就退出进程，让容器去自动重启
            throw new RuntimeException(ex);
        }
        mqttClient = client;
    }

    public long getArrivedSize() {
        return arrivedSize.get();
    }

    private final AtomicBoolean retryIng = new AtomicBoolean(false);


    @Override
    public void connectionLost(Throwable cause) {
        log.error("connectionLost", cause);
        lossRecord.update(cause);// update metric
        if (retryIng.compareAndSet(false, true)) {
            scheduledExecutorService.schedule(reSubscriber, 10, TimeUnit.MILLISECONDS);
        }
    }

    private void reconnectAndSubscribe(String reason) throws MqttException {
        log.info("try reconnect {}", reason);
        String msg;
        try {
            mqttClient.reconnect();
            msg = "reconnect";
        } catch (MqttException ex) {
            int reasonCode = ex.getReasonCode();
            if (reasonCode != MqttException.REASON_CODE_CLIENT_CONNECTED) {
                TopologyLog.log(TopologyLog.Node.PULL_MQTT, TopologyLog.EventCode.ERROR, I18nUtils.getMessage("uns.topology.mqtt.init"));
                throw ex;
            } else {
                msg = "Connected";
            }
        }
        log.info("{} Success, reason: {}", msg, reason);
        if (!subscribeTopics.isEmpty()) {
            mqttClient.subscribe(subscribeTopics.toArray(new String[0]));// 重新订阅
        }
    }

    public void reconnect() throws MqttException {
        reconnectAndSubscribe("fromAPI");
        lossRecord.setLastReconnectTime(System.currentTimeMillis());
    }

    public String getClientId() {
        return clientId;
    }

    private final ConnectionLossRecord lossRecord = new ConnectionLossRecord();

    public ConnectionLossRecord getConnectionLossRecord() {
        return lossRecord.clone();
    }


    private Runnable reSubscriber = new Runnable() {
        final AtomicInteger retry = new AtomicInteger();

        @Override
        public void run() {
            int index = retry.incrementAndGet();
            try {
                lossRecord.lastReconnect();
                reconnectAndSubscribe("connectionLost " + index);
                lossRecord.lastReconnectSuccess();
                retry.set(0);
                retryIng.set(false);
            } catch (MqttException e) {
                TopologyLog.log(TopologyLog.Node.PULL_MQTT, TopologyLog.EventCode.ERROR, I18nUtils.getMessage("uns.topology.mqtt.init"));
                log.error("reconnectErr: " + mqttClient.getServerURI(), e);
                scheduledExecutorService.schedule(this, index, TimeUnit.SECONDS);
            }
        }
    };

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        arrivedSize.incrementAndGet();
        try {
            messageConsumer.onMessage(topic, message.getId(), message.getPayload());
        } catch (Throwable ex) {
            TopologyLog.log(TopologyLog.Node.PULL_MQTT, TopologyLog.EventCode.ERROR, I18nUtils.getMessage("uns.topology.mqtt.consume"));
            log.error("messageConsumeErr: topic=" + topic + ", payload=" + new String(message.getPayload()), ex);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.trace("deliveryComplete: {}", token);
    }

    @Override
    public void publishMessage(String topic, byte[] msg, int qos) throws MqttException {
        MqttMessage message = new MqttMessage(msg);
        message.setQos(qos);
        mqttClient.publish(topic, message);
    }

    @Override
    public void subscribe(Collection<String> prev, boolean throwException) {
        if (CollectionUtil.isEmpty(prev)) {
            return;
        }
        ArrayList<String> newAdds = new ArrayList<>(Math.max(16, prev.size()));
        for (String topic : prev) {
            if (subscribeTopics.add(topic)) {
                newAdds.add(topic);
            }
        }
        if (!newAdds.isEmpty()) {
            try {
                mqttClient.subscribe(newAdds.toArray(new String[0]));
            } catch (MqttException e) {
                if (throwException) {
                    newAdds.forEach(subscribeTopics::remove);
                    throw new RuntimeException(e);
                }
                log.error("订阅失败:" + newAdds, e);
                lossRecord.update(e);// update metric
                if (retryIng.compareAndSet(false, true)) {
                    scheduledExecutorService.schedule(reSubscriber, 10, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    public void unSubscribe(Collection<String> topics) {
        if (CollectionUtil.isEmpty(topics)) {
            return;
        }
        subscribeTopics.removeAll(topics);
        try {
            mqttClient.unsubscribe(topics.toArray(new String[0]));
        } catch (MqttException e) {
            log.error("取消订阅失败:", e);
            try {
                mqttClient.disconnectForcibly(1);
            } catch (MqttException ex) {
                log.error("主动断开失败!");
            }
            lossRecord.update(e);// update metric
            if (retryIng.compareAndSet(false, true)) {
                scheduledExecutorService.schedule(reSubscriber, 10, TimeUnit.MILLISECONDS);
            }
        }
    }

}
