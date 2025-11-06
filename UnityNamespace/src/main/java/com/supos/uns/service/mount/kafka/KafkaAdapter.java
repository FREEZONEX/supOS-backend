package com.supos.uns.service.mount.kafka;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.mount.meta.connection.ConnectionReq;
import com.supos.common.dto.mount.meta.connection.ConnectionResp;
import com.supos.common.enums.mount.MountMetaQueryType;
import com.supos.common.enums.mount.MountModel;
import com.supos.common.enums.mount.MountSourceType;
import com.supos.common.event.EventBus;
import com.supos.common.event.mount.ConnectMountMetaQueryEvent;
import com.supos.common.event.mount.MountSourceOnlineEvent;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.service.mount.MountCoreService;
import com.supos.uns.service.mount.MountUtils;
import com.supos.uns.service.mount.mqtt.MqttMetaChangeManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: kafka服务适配器
 * @date 2025/10/10 10:40
 */
@Slf4j
public class KafkaAdapter {

    private static final String DEFAULT_GROUP_ID = "uns-mount-kafka-group";
    private static final Set<String> IGNORE_TOPICS = Set.of("__consumer_offsets", "__transaction_state");

    /**{连接名:kafka消费对象}*/
    private final Map<String, KafkaConsumer<String, String>> consumerMap = new ConcurrentHashMap<>();
    /**{连接名:kafka消费状态}*/
    private final Map<String, AtomicBoolean> consumingStatusMap = new ConcurrentHashMap<>();

    /**{连接名:{topic:最新消息}}*/
    @Getter
    private final Map<String, Map<String, String> > messageMap = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, List<String>> topicMap = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();

    private MountCoreService mountCoreService;

    public KafkaAdapter(MountCoreService mountCoreService) {
        this.mountCoreService = mountCoreService;
    }

    /**
     * 查询kafka在离线状态
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

        MountSourceOnlineEvent event = new MountSourceOnlineEvent(this, MountSourceType.KAFKA, unsMountPo.getSourceAlias(), callback);
        EventBus.publishEvent(event);

        if (!success.get()) {
            throw new RuntimeException("query kafka online error");
        }
        return result.get();
    }

    /**
     * 获取kafka连接信息
     * @param connectName
     * @return
     */
    public ConnectionResp queryKafka(String connectName) {
        AtomicBoolean success = new AtomicBoolean(true);
        AtomicReference<ConnectionResp> result = new AtomicReference<>();

        ConnectionReq param = new ConnectionReq();
        param.setName(connectName);

        Consumer<Object> callback = (resp) -> {
            if (resp == null) {
                success.set(false);
            }
            result.set((ConnectionResp) resp);
        };

        ConnectMountMetaQueryEvent event = new ConnectMountMetaQueryEvent(this, null, MountMetaQueryType.KAFKA_BROKER,
                param, callback);
        EventBus.publishEvent(event);

        if (!success.get()) {
            throw new RuntimeException("query kafka connect info error");
        }

        return result.get();
    }

    /**
     * 连接Kafka服务
     *
     * @param config 服务连接信息
     * @return 是否连接成功
     */
    public boolean connect(JSONObject config) {
        String connectName = config.getString("connectName");
        try {
            KafkaConsumer<String, String> consumer = consumerMap.get(connectName);
            if (consumer != null) {
                log.info("Kafka consumer already exists for source: {}", connectName);
                return true;
            }

            // 构建Kafka消费者配置
            Properties props = buildKafkaProperties(config);

            // 创建Kafka消费者实例
            consumer = new KafkaConsumer<>(props);

            // 获取所有topic并订阅
            Set<String> allTopics = consumer.listTopics().keySet()
                    .stream()
                    .filter(topic -> !IGNORE_TOPICS.contains(topic))
                    .collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(allTopics)) {
                consumer.subscribe(allTopics);
                topicMap.put(connectName, allTopics.stream().sorted().collect(Collectors.toList()));
            } else {
                topicMap.put(connectName, Collections.EMPTY_LIST);
            }

            // 存储消费者实例
            consumerMap.put(connectName, consumer);
            consumingStatusMap.put(connectName, new AtomicBoolean(false));

            log.info("Successfully connected to Kafka server for source: {}, topics: {}", connectName, StringUtils.join(allTopics, ", "));
            return true;
        } catch (Exception e) {
            log.error("Failed to connect to Kafka server for source: {}", connectName, e);
            return false;
        }
    }

    /**
     * 断开Kafka连接
     *
     * @param config 数据源配置
     */
    public void disconnect(JSONObject config) {
        String connectName = config.getString("connectName");
        KafkaConsumer<String, String> consumer = consumerMap.remove(connectName);
        stopConsuming(config);
        if (consumer != null) {
            try {
                consumingStatusMap.remove(connectName);
                consumer.unsubscribe();
                consumer.close();
                messageMap.remove(connectName);
                topicMap.remove(connectName);

                log.info("Disconnected from Kafka server for source: {}", connectName);
            } catch (Exception e) {
                log.error("Error while disconnecting from Kafka server for source: {}", connectName, e);
            }
        }
    }

    /**
     * 启动topic变更监听，自动调整订阅
     *
     * @param config 数据源配置
     */
    public void topicChange(JSONObject config) {
        String connectName = config.getString("connectName");
        KafkaConsumer<String, String> consumer = consumerMap.get(connectName);
        if (consumer == null) {
            throw new IllegalStateException("Kafka consumer not found for source: " + connectName);
        }

        // 监控topic变化
        w.lock();
        try {
            Set<String> newTopics = consumer.listTopics().keySet()
                    .stream()
                    .filter(topic -> !IGNORE_TOPICS.contains(topic))
                    .collect(Collectors.toSet());
            List<String> currentTopics = newTopics.stream().sorted().collect(Collectors.toList());
            List<String> previousTopics = topicMap.get(connectName);
            if (!CollectionUtil.isEqualList(topicMap.get(connectName), currentTopics)) {
                log.info("Previous topics: {}, Current topics: {}", previousTopics, currentTopics);
                consumer.subscribe(currentTopics);
                topicMap.put(connectName, currentTopics);
            }
        } finally {
            w.unlock();
        }

    }

    /**
     * 开始消费消息
     *
     * @param config 数据源配置
     */
    public void startConsuming(JSONObject config) {
        String connectName = config.getString("connectName");
        KafkaConsumer<String, String> consumer = consumerMap.get(connectName);
        if (consumer == null) {
            throw new IllegalStateException("Kafka consumer not found for source: " + connectName);
        }

        AtomicBoolean isConsuming = consumingStatusMap.get(connectName);
        if (isConsuming == null || isConsuming.get()) {
            log.warn("Consumer is already running for source: {}", connectName);
            return;
        }

        isConsuming.set(true);

        // 启动独立线程消费消息
        Thread consumerThread = new Thread(() -> {
            try {
                while (isConsuming.get() && !Thread.currentThread().isInterrupted()) {
                    r.lock();
                    try {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                        for (ConsumerRecord<String, String> record : records) {
                            try {
                                messageMap.computeIfAbsent(connectName, k -> new ConcurrentHashMap<>()).put(record.topic(), record.value());
                                log.debug("Received message from topic: {}, partition: {}, offset: {}, value: {}",
                                        record.topic(), record.partition(), record.offset(), record.value());
                                String alias = MountUtils.alias(MountSourceType.KAFKA, connectName, record.topic());
                                CreateTopicDto file = mountCoreService.getDefinitionByAlias(alias);
                                if (file == null) {
                                    List<UnsMountPo> mountPos = mountCoreService.queryMountInfo(MountModel.KAFKA_ALL, null,null);
                                    KafkaMountAdpter adapter = mountCoreService.getAdapter(KafkaMountAdpter.class);
                                    if (!mountPos.isEmpty() && adapter != null) {
                                        adapter.getKafkaMetaChangeManager().handleTopic(mountPos.get(0), connectName);
                                    }
                                }
                                mountCoreService.saveTopicPayloadToUns(MountSourceType.KAFKA, connectName, record.topic(), record.value());
                            } catch (Exception e) {
                                log.error("Error processing Kafka message from topic: {}, partition: {}, offset: {}",
                                        record.topic(), record.partition(), record.offset(), e);
                            }
                        }
                        consumer.commitAsync();
                        Thread.sleep(10);
                    } catch (Exception e) {
                        log.error("Error polling messages for source: {}", connectName, e);
                    } finally {
                        r.unlock();
                    }
                }
            } finally {
                isConsuming.set(false);
            }
        }, "KafkaConsumer-" + connectName);

        consumerThread.setDaemon(true);
        consumerThread.start();

        log.info("Started consuming messages for source: {}", connectName);
    }

    /**
     * 停止消费消息
     *
     * @param config 数据源配置
     */
    public void stopConsuming(JSONObject config) {
        String connectName = config.getString("connectName");
        AtomicBoolean isConsuming = consumingStatusMap.get(connectName);
        if (isConsuming != null) {
            isConsuming.set(false);
        }
        log.info("Stopped consuming messages for source: {}", connectName);
    }

    /**
     * 构建Kafka消费者属性配置
     *
     * @param config 数据源配置
     * @return Kafka配置属性
     */
    private Properties buildKafkaProperties(JSONObject config) {
        Properties props = new Properties();

        // 设置Bootstrap Servers
        String nodeHosts = config.getString("nodeHosts");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, nodeHosts);

        // 设置Key和Value的反序列化器
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // 设置消费者组ID
        props.put(ConsumerConfig.GROUP_ID_CONFIG, DEFAULT_GROUP_ID + new Random().nextInt(1000));

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        // 设置会话超时时间
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");

        // TODO 账号密码
        return props;
    }

    /**
     * 查询消费者状态
     *
     * @param sourceAlias 数据源别名
     * @return 是否正在消费
     */
    public boolean isConsuming(String sourceAlias) {
        AtomicBoolean isConsuming = consumingStatusMap.get(sourceAlias);
        return isConsuming != null && isConsuming.get();
    }

    /**
     * 获取消费者位置信息
     *
     * @param sourceAlias 数据源别名
     * @return 各分区的当前位置
     */
    public Map<TopicPartition, Long> getConsumerPositions(String sourceAlias) {
        KafkaConsumer<String, String> consumer = consumerMap.get(sourceAlias);
        if (consumer == null) {
            return Collections.emptyMap();
        }

        Map<TopicPartition, Long> positions = new HashMap<>();
        try {
            Set<TopicPartition> assignment = consumer.assignment();
            for (TopicPartition partition : assignment) {
                positions.put(partition, consumer.position(partition));
            }
        } catch (Exception e) {
            log.error("Error getting consumer positions for source: {}", sourceAlias, e);
        }
        return positions;
    }
}
