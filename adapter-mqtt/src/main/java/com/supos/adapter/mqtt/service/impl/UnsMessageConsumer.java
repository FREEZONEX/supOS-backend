package com.supos.adapter.mqtt.service.impl;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.thread.RejectPolicy;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.cron.timingwheel.TimerTask;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.bluejeans.common.bigqueue.BigArray;
import com.bluejeans.common.bigqueue.BigQueue;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.supos.adapter.mqtt.dto.FieldErrMsg;
import com.supos.adapter.mqtt.dto.LastMessage;
import com.supos.adapter.mqtt.dto.TopicDefinition;
import com.supos.adapter.mqtt.dto.TopicMessage;
import com.supos.adapter.mqtt.service.MQTTPublisher;
import com.supos.adapter.mqtt.service.MessageConsumer;
import com.supos.adapter.mqtt.util.SystemWheeledTimer;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.TopicMessageConsumer;
import com.supos.common.annotation.Description;
import com.supos.common.dto.*;
import com.supos.common.enums.FieldType;
import com.supos.common.event.*;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.codahale.metrics.MetricRegistry.name;

@Service
@Slf4j
public class UnsMessageConsumer implements MessageConsumer, TopicMessageConsumer, IUnsDefinitionService {
    private final ConcurrentHashMap<Long, TopicDefinition> topicDefinitionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> aliasMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> pathMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> topicMap = Constants.useAliasAsTopic ? aliasMap : pathMap;

    private final ConcurrentHashSet<Long> scheduleCalcTopics = new ConcurrentHashSet<>();

    public Map<Long, TopicDefinition> getTopicDefinitionMap() {
        return topicDefinitionMap;
    }

    private final ExecutorService dataPublishExecutor = ThreadUtil.newFixedExecutor(Integer.parseInt(System.getProperty("sink.thread", "4")), 100, "dbPub-", RejectPolicy.CALLER_RUNS.getValue());
    private final ExecutorService topicSender = ThreadUtil.newFixedExecutor(2, 100, "topicSend-", RejectPolicy.CALLER_RUNS.getValue());

    private static final String QUEUE_DIR = Constants.LOG_PATH + File.separator + "queue";
    private final BigQueue queue;

    {
        BigQueue Q = new BigQueue(QUEUE_DIR, "mqtt_queue_cache", 64 * 1024 * 1024);
        try {
            Q.peek();
        } catch (Throwable ex) {
            File dir = new File(QUEUE_DIR);
            // windows系统容易出现文件占用删不掉，此时换个目录
            boolean del = FileUtils.deleteDir(dir);
            log.error("onStart 队列不可用(size={})，清空重建! {}, del: {}", Q.size(), ex.getMessage(), del);
            String q2 = del ? "mqtt_queue_cache" : ("mqtt_queue_" + DateTimeUtils.dateSimple());
            if (!del) {
                dir.deleteOnExit();
            }
            Q = new BigQueue(QUEUE_DIR, q2, 64 * 1024 * 1024);
        }
        queue = Q;
    }

    private final ScheduledExecutorService queueConsumer = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        final AtomicInteger threadNum = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "BQConsumer-" + threadNum.incrementAndGet());
        }
    });

    private final SystemWheeledTimer systemTimer = new SystemWheeledTimer();
    private MQTTPublisher mqttPublisher;

    private final AtomicLong enqueuedSize = new AtomicLong();
    private final AtomicLong dequeuedSize = new AtomicLong();

    private final AtomicLong publishedCalcSize = new AtomicLong();
    private final AtomicLong arrivedCalcSize = new AtomicLong();

    private final AtomicLong publishedMergeSize = new AtomicLong();

    public long getEnqueuedSize() {
        return enqueuedSize.get();
    }

    public long getDequeuedSize() {
        return dequeuedSize.get();
    }

    public long getPublishedCalcSize() {
        return publishedCalcSize.get();
    }

    public long getPublishedMergedSize() {
        return publishedMergeSize.get();
    }

    public long getArrivedCalcSize() {
        return arrivedCalcSize.get();
    }

    public void setMqttPublisher(MQTTPublisher mqttPublisher) {
        this.mqttPublisher = mqttPublisher;
    }

    public UnsMessageConsumer() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                queue.gc();
                if (queue.isEmpty()) {
                    queue.removeAll();
                }
                queue.close();
            } catch (IOException e) {
                log.error("Fail to Close BigQueue", e);
            }
        }));
    }

    public static int FETCH_SIZE = Integer.parseInt(System.getProperty("fetch", "8000"));
    public static int MAX_WAIT_MILLS = Integer.parseInt(System.getProperty("maxWait", "1000"));

    private boolean subscribeALL;

    @EventListener(classes = ContextRefreshedEvent.class)
    @Order(1)
    void init() {
        if (mqttPublisher == null) {
            try {
                mqttPublisher = SpringUtil.getBean(MQTTPublisher.class);
            } catch (Exception be) {
                log.warn("使用 Mock MQTTPublisher");
                mqttPublisher = new MQTTPublisher() {
                    @Override
                    public void publishMessage(String topic, byte[] msg, int qos) {
                        onMessage(topic, -1, msg);
                    }

                    @Override
                    public void subscribe(Collection<String> topics, boolean throwException) {
                    }

                    @Override
                    public void unSubscribe(Collection<String> topics) {
                    }
                };
            }
        }
        mqttPublisher.unSubscribe(List.of(Constants.RESULT_TOPIC_PREV + "#"));
        try {
            mqttPublisher.subscribe(List.of("#"), true);
            subscribeALL = true;
        } catch (Exception ex) {
            log.warn("mqtt 无法订阅所有 {}", ex.getMessage());
        }

        systemTimer.start();
        queueConsumer.schedule((Runnable) () -> {
            TreeMap<SrcJdbcType, HashMap<Long, SaveDataDto>> typedDataMap = new TreeMap<>();
            while (true) {
                try {
                    fetchData(typedDataMap);
                } catch (InterruptedException ec) {
                    log.warn("消費已取消!");
                } catch (Exception e) {
                    log.error("消费异常", e);
                    try {
                        queue.peek();
                    } catch (Throwable ex) {
                        log.error("队列不可用(size={})，清空重建! {}", queue.size(), ex.getMessage());
                        queueLocker.writeLock().lock();
                        try {
                            queue.removeAll();
                            int permits = this.semaphore.availablePermits();
                            if (permits > 0) {
                                boolean ok = this.semaphore.tryAcquire(permits);
                                log.warn("丢弃许可: {} {}, size={}", permits, ok, queue.size());
                            } else {
                                log.warn("重建后: size={}", queue.size());
                            }
                        } finally {
                            queueLocker.writeLock().unlock();
                        }

                    }
                } finally {
                    queue.gc();
                }
            }
        }, 2, TimeUnit.SECONDS);
    }

    private void fetchData(Map<SrcJdbcType, HashMap<Long, SaveDataDto>> typedDataMap) throws Exception {
        int headSpendPerms = 0;
        if (queue.isEmpty()) {
            semaphore.acquire();
            headSpendPerms = 1;
        }
        final int fetchSize = FETCH_SIZE;
        final int maxWait = MAX_WAIT_MILLS;
        List<byte[]> dataList = new ArrayList<>(fetchSize);
        final long fetchStartTime = System.currentTimeMillis();
        int spendPerms = 0, tryPerms;
        long maxWaitMills;
        while ((tryPerms = fetchSize - dataList.size()) > 0 && (maxWaitMills = maxWait - (System.currentTimeMillis() - fetchStartTime)) > 0) {
            if (semaphore.tryAcquire(tryPerms, maxWaitMills, TimeUnit.MILLISECONDS)) {
                spendPerms += tryPerms;
            }
            if (!queue.isEmpty()) {
                List<byte[]> fetched = queue.dequeueMulti(tryPerms);
                if (!fetched.isEmpty()) {
                    dataList.addAll(fetched);
                }
            }
        }
        int leftPerms = dataList.size() - spendPerms - headSpendPerms;
        if (leftPerms > 0) {
            semaphore.acquire(leftPerms);
        }
        if (!dataList.isEmpty()) {
            dequeuedSize.getAndAdd(dataList.size());
            for (byte[] dataBs : dataList) {
                String json = new String(dataBs, StandardCharsets.UTF_8);
                TopicMessage msg = JSON.parseObject(json, TopicMessage.class);
                Long id = msg.getId();
                TopicDefinition definition = topicDefinitionMap.get(id);
                if (definition != null) {
                    List<Map<String, Object>> list = msg.getMsg();
                    HashMap<Long, SaveDataDto> topicData = typedDataMap.computeIfAbsent(definition.getJdbcType(), k -> new HashMap<>());
                    SaveDataDto dataDto = topicData.computeIfAbsent(id, k -> new SaveDataDto(id, definition.getTable(), definition.getFieldDefines(), new LinkedList<>()));
                    CreateTopicDto def = definition.getCreateTopicDto();
                    dataDto.setCreateTopicDto(def);
                    dataDto.getList().addAll(list);
                    String fieldValue = def.getTbFieldName();
                    if (fieldValue != null) {
                        final String curUns = def.getAlias();
                        for (Map<String, Object> data : list) {
                            data.put(fieldValue, curUns);
                        }
                    }
                }
            }
            putByCt(typedDataMap);
            sendData(typedDataMap);
        }
    }

    private static void putByCt(Map<SrcJdbcType, HashMap<Long, SaveDataDto>> typedDataMap) {
        for (Map.Entry<SrcJdbcType, HashMap<Long, SaveDataDto>> entry : typedDataMap.entrySet()) {
            Collection<SaveDataDto> vs = entry.getValue().values();
            for (SaveDataDto dto : vs) {
                List<Map<String, Object>> list = dto.getList();
                Collections.reverse(list);
                Iterator<Map<String, Object>> iterator = list.iterator();
                Object prevCt = null;
                Map<String, Object> prevBean = null;
                final String CT = dto.getCreateTopicDto().getTimestampField();
                while (iterator.hasNext()) {
                    Map<String, Object> data = iterator.next();
                    Object ct = data.get(CT);
                    if (ct != null && ct.equals(prevCt)) {
                        log.debug("删除时间重复数据: {}", data);
                        iterator.remove();
                        prevBean.replace(Constants.MERGE_FLAG, 1, 2);
                    }
                    prevCt = ct;
                    prevBean = data;
                }
                Collections.reverse(list);
            }
        }
    }

    private void sendData(Map<SrcJdbcType, HashMap<Long, SaveDataDto>> typedDataMap) {

        for (Map.Entry<SrcJdbcType, HashMap<Long, SaveDataDto>> entry : typedDataMap.entrySet()) {

            TreeMap<TopicDefinition, SaveDataDto> calcMap = new TreeMap<>(Comparator.comparingInt(TopicDefinition::getDataType) // 假如有以下依赖关系，告警->计算实例->时序实例，保障按依赖顺序排序
                    .thenComparingInt(System::identityHashCode));

            SrcJdbcType jdbcType = entry.getKey();
            HashMap<Long, SaveDataDto> topicData = entry.getValue();
            long countRecords = 0;
            Iterator<Map.Entry<Long, SaveDataDto>> itr = topicData.entrySet().iterator();
            while (itr.hasNext()) {
                SaveDataDto dto = itr.next().getValue();
                Long id = dto.getId();
                TopicDefinition definition = topicDefinitionMap.get(id);
                if (definition == null) {
                    log.warn("{} 已被删除!", id);
                    itr.remove();
                    continue;
                }
                countRecords += dto.getList().size();
                if (definition.getCompileExpression() != null) {
                    arrivedCalcSize.getAndAdd(dto.getList().size());
                }
                Set<Long> calcTopics = definition.getReferCalcUns();
                if (calcTopics != null && calcTopics.size() > 0) {
                    for (Long calcTopic : calcTopics) {
                        TopicDefinition calc = topicDefinitionMap.get(calcTopic);
                        if (calc != null) {
                            calcMap.compute(calc, (k, oldV) -> oldV == null || oldV.getList().size() < dto.getList().size() ? dto : oldV);
                        } else {
                            log.debug("calc TopicDefinitionNotFound: {}", calcTopic);
                        }
                    }
                }
            }
            this.computeCalcTopic(calcMap, topicData);

            HashMap<String, SaveDataDto> tableData = new HashMap<>(topicData.size());
            for (SaveDataDto d : topicData.values()) {
                TopicDefinition definition = topicDefinitionMap.get(d.getId());
                if (definition != null && definition.isSave2db()) {
                    String table = d.getTable();
                    SaveDataDto data = tableData.get(table);
                    if (data != null) {
                        data.getList().addAll(d.getList());
                    } else {
                        tableData.put(table, d);
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("publishData: {}, size={}, dataLen={}, topics={}", jdbcType.name(), countRecords, tableData.size(), tableData.keySet());
            }
            topicData.clear();
            if (tableData.size() > 0) {
                SaveDataEvent event = new SaveDataEvent(this, jdbcType, tableData.values().toArray(new SaveDataDto[0]));
                dataPublishExecutor.submit(() -> EventBus.publishEvent(event));
            }
        }
    }

    private void computeCalcTopic(Map<TopicDefinition, SaveDataDto> calcMap, HashMap<Long, SaveDataDto> topicData) {
        AtomicInteger count = new AtomicInteger(0);
        for (Map.Entry<TopicDefinition, SaveDataDto> entry : calcMap.entrySet()) {
            count.set(0);
            TopicDefinition calc = entry.getKey();
            SaveDataDto dto = entry.getValue();
            String calcTopic = calc.getTopic();
            final String CT = calc.getCreateTopicDto().getTimestampField();
            final String qosField = calc.getCreateTopicDto().getQualityField();
            Map<Long, Object[]> calcRs = tryCalc(topicDefinitionMap, calc, dto, topicData, count);
            if (calcRs != null) {
                Object oldRsValue = null;
                Object oldValue = null;
                Long lastAlarmTime = null;

                FieldDefine calcField = calc.getFieldDefines().getCalcField();
                for (Map.Entry<Long, Object[]> rsEntry : calcRs.entrySet()) {
                    Long minTime = rsEntry.getKey();
                    Object[] vs = rsEntry.getValue();
                    Object evalRs = vs[0];// 计算表达式的值
                    if (evalRs == null) {
                        continue;
                    }
                    Map<String, Object> vars = (Map<String, Object>) vs[1];// 引用的其他变量值
                    StringBuilder jsonVal = new StringBuilder(128);
                    jsonVal.append("{\"").append(CT).append("\":").append(minTime).append(",\"").append(calcField.getName()).append("\":");
                    if (evalRs instanceof Long) {
                        jsonVal.append(evalRs);
                    } else {
                        jsonVal.append("\"").append(evalRs).append('"');
                    }
                    if (vs[2] != null && qosField != null) {
                        jsonVal.append(",\"").append(qosField).append("\":").append(vs[2]);
                    }
                    if (vars.size() == 1 && calc.getFieldDefines().getFieldsMap().containsKey(AlarmRuleDefine.FIELD_CURRENT_VALUE)) {
                        AlarmRuleDefine alarmRuleDefine = calc.getAlarmRuleDefine();
                        Object currentValue = vars.values().iterator().next();
                        if (alarmRuleDefine != null) { // 告警的情况
                            Map<String, Object> lastMsg = calc.getLastMsg();
                            if (oldRsValue == null && lastMsg != null) {
                                oldRsValue = lastMsg.get(AlarmRuleDefine.FIELD_IS_ALARM);
                                if (oldRsValue != null && !(oldRsValue instanceof Boolean)) {
                                    oldRsValue = Boolean.valueOf(oldRsValue.toString());
                                }
                                oldValue = lastMsg.get(AlarmRuleDefine.FIELD_CURRENT_VALUE);
                            }
                            if (!(evalRs instanceof Boolean)) {
                                evalRs = Boolean.valueOf(evalRs.toString());
                            }
                            final boolean isAlarm = (Boolean) evalRs;
                            if (evalRs.equals(oldRsValue)) {
                                log.debug("告警表达式结果没变，忽略：val={}, topic={}, rs = {}", currentValue, calc.getTopic(), evalRs);
                                continue;
                            } else if (oldRsValue == null && !isAlarm) {
                                log.info("忽略首次告警消失：val={}, topic={}", currentValue, calc.getTopic());
                                continue;
                            }
                            Double deadBand = alarmRuleDefine.getDeadBand();
                            Long overTime = alarmRuleDefine.getOverTime();
                            if (!isAlarm && oldValue != null && deadBand != null) {
                                Double d1 = Double.parseDouble(currentValue.toString());
                                Double d2 = Double.parseDouble(oldValue.toString());

                                Integer deadBandType = alarmRuleDefine.getDeadBandType();
                                if (deadBandType == null) {
                                    deadBandType = 1;
                                }
                                double diff = deadBand;
                                if (deadBandType == 2) {//百分比
                                    diff = deadBand / 100;
                                }
                                if (Math.abs(d1 - d2) < diff) {
                                    log.debug("告警在死区，忽略：val={}, topic={}", currentValue, calc.getTopic());
                                    continue;
                                }
                            } else if (isAlarm && overTime != null) {

                                if (lastAlarmTime == null && lastMsg != null) {
                                    Object lastTimeObj = lastMsg.get(CT);
                                    if (lastTimeObj instanceof Long) {
                                        lastAlarmTime = DateTimeUtils.convertToMills((Long) lastTimeObj);
                                    }
                                }
                                if (lastAlarmTime != null) {
                                    //统一单位为毫秒
                                    long timeB = DateTimeUtils.convertToMills(minTime);
                                    long diffSeconds = Math.abs(lastAlarmTime - timeB) / 1000;
                                    if (diffSeconds < overTime) {
                                        log.debug("告警在越限时长，忽略：val={}, topic={}", currentValue, calc.getTopic());
                                        continue;
                                    }
                                }
                            }
                            if (lastMsg != null) {
                                lastMsg.put(AlarmRuleDefine.FIELD_IS_ALARM, evalRs);
                                lastMsg.put(AlarmRuleDefine.FIELD_CURRENT_VALUE, currentValue);
                            }
                            oldRsValue = evalRs;
                            oldValue = currentValue;
                            if (isAlarm) {
                                lastAlarmTime = System.currentTimeMillis();
                            }

                            jsonVal.append(",\"").append(AlarmRuleDefine.FIELD_LIMIT_VALUE).append("\":\"").append(alarmRuleDefine.getLimitValue()).append('"');
                            jsonVal.append(",\"").append(AlarmRuleDefine.FIELD_UNS_ID).append("\":").append(calc.getCreateTopicDto().getId());
                            jsonVal.append(",\"").append(AlarmRuleDefine.FIELD_ID).append("\":").append(AlarmRuleDefine.nextId());
                        }
                        jsonVal.append(",\"").append(AlarmRuleDefine.FIELD_CURRENT_VALUE).append("\":").append(currentValue);
                    }
                    jsonVal.append('}');
                    String msg = jsonVal.toString();
                    topicSender.submit(() -> {
                        log.debug("发送计算值：{}: {}", calcTopic, msg);
                        try {
                            mqttPublisher.publishMessage(calcTopic, msg.getBytes(StandardCharsets.UTF_8), 0);
                            publishedCalcSize.incrementAndGet();
                        } catch (MqttException e) {
                            log.error("ErrPublishCalcMsg: topic=" + calcTopic + ", data=" + jsonVal, e);
                        }
                    });
                }
            } else {
                log.warn("NO 计算值：{}", calcTopic);
            }
        }
    }


    void uncaughtException(Thread t, Throwable e) {
        log.error("发送数据失败: " + t.getName(), e);
    }


    public long getQueueSize() {
        return queue.size();
    }

    public long getQueueFrontIndex() {
        try {
            Field field = queue.getClass().getDeclaredField("queueFrontIndex");
            field.setAccessible(true);
            AtomicLong queueFrontIndex = (AtomicLong) field.get(queue);
            return queueFrontIndex.get();
        } catch (Exception e) {
            return -1;
        }
    }

    public long getQueueTailIndex() {
        try {
            Field field = queue.getClass().getDeclaredField("innerArray");
            field.setAccessible(true);
            BigArray queueFrontIndex = (BigArray) field.get(queue);
            return queueFrontIndex.getHeadIndex();
        } catch (Exception e) {
            return -1;
        }
    }

    public String getQueueHead() {
        byte[] head = queue.peek();
        if (head != null && head.length > 0) {
            return new String(head, StandardCharsets.UTF_8);
        }
        return null;
    }

    private final Semaphore semaphore;

    {
        try {
            queue.peek();
        } catch (Throwable ex) {
            log.error("队列不可用，清空重建! {}", ex.getMessage());
            queue.removeAll();
        }
        long queueSize = queue.size();
        if (queueSize < Integer.MAX_VALUE) {
            log.info("队列初始尺寸：{}", queueSize);
            semaphore = new Semaphore((int) queueSize);
            enqueuedSize.set(queueSize);
        } else {
            log.warn("队列初始尺寸太大：{}, 准备清空重建..", queueSize);
            queue.removeAll();
            semaphore = new Semaphore(0);
        }
    }

    private final ReadWriteLock queueLocker = new ReentrantReadWriteLock(true);
    private static final byte GMQTT_MAGIC_HEAD0 = 9;
    private static final byte GMQTT_MAGIC_HEAD1 = 7;

    private static long bigEndianBytesToUInt32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFFL) << 24) | ((bytes[offset + 1] & 0xFFL) << 16) | ((bytes[offset + 2] & 0xFFL) << 8) | (bytes[offset + 3] & 0xFFL);
    }

    private final LastMessage lastMsg = new LastMessage();

    public LastMessage getLastMessage() {
        return lastMsg.clone();
    }

    private static final String MERGE_TOPIC = SystemUtil.get("MERGE_TOPIC", "4174348a-9222-4e81-b33e-5d72d2fd7f1e");

    private TopicDefinition getDefinition(String topic) {
        TopicDefinition definition = null;
        Long unsId;
        if (Character.isDigit(topic.charAt(0))) {
            try {
                unsId = Long.parseLong(topic);
                definition = topicDefinitionMap.get(unsId);
                if (definition == null) {
                    unsId = topicMap.get(topic);
                }
            } catch (NumberFormatException ex) {
                unsId = topicMap.get(topic);
            }
        } else {
            unsId = topicMap.get(topic);
            if (unsId == null) {
                unsId = Constants.useAliasAsTopic ? pathMap.get(topic) : aliasMap.get(topic);
            }
        }
        if (definition == null && unsId != null) {
            definition = topicDefinitionMap.get(unsId);
        }
        return definition;
    }

    @Override
    public void onMessage(String topic, int msgId, byte[] bytes) {
        TopicDefinition definition = getDefinition(topic);
        Long unsId;
        if (Character.isDigit(topic.charAt(0))) {
            try {
                unsId = Long.parseLong(topic);
                definition = topicDefinitionMap.get(unsId);
                if (definition == null) {
                    unsId = topicMap.get(topic);
                }
            } catch (NumberFormatException ex) {
                unsId = topicMap.get(topic);
            }
        } else {
            unsId = topicMap.get(topic);
            if (unsId == null) {
                unsId = aliasMap.get(topic);
            }
        }
        if (definition == null && unsId != null) {
            definition = topicDefinitionMap.get(unsId);
        } else if (definition == null && MERGE_TOPIC.equals(topic)) {
            try {
                JSONArray array = JSON.parseArray(new String(bytes, StandardCharsets.UTF_8));
                for (Object o : array) {
                    if (o instanceof Map<?, ?> map) {
                        Object data = map.remove("data");
                        if (data != null && !map.isEmpty()) {
                            String alias = String.valueOf(map.values().iterator().next());
                            TopicDefinition liDef = getDefinition(alias);
                            if (liDef != null) {
                                onMessage(liDef, alias, -1, data.toString().getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("mergeTopicBadFmt", ex);
            }
            return;
        }
        onMessage(definition, topic, msgId, bytes);
    }


    private void onMessage(TopicDefinition definition, String topic, int msgId, byte[] bytes) {
        if (definition == null) {
            String payload = new String(bytes, StandardCharsets.UTF_8);
            log.debug("TopicDefinition NotFound[{}] : payload = {}", topic, payload);
            if (subscribeALL) {
                TopicMessageEvent event = new TopicMessageEvent(this, null, null, -1, topic, payload);
                dataPublishExecutor.submit(() -> {
                    this.topicSender.submit(() -> EventBus.publishEvent(event));
                });
            }
            return;
        }
        requestCounter.inc();

        if (definition.getDataType() == Constants.CITING_TYPE) {
            log.warn("拒绝引用类型写值: topic={}", topic);
            return;
        }
        final Long unsId = definition.getCreateTopicDto().getId();
        FieldDefines fieldDefines = definition.getFieldDefines();
        final Instant nowInstant = Instant.now();
        final long nowInMills = nowInstant.toEpochMilli();
        boolean preProcessed = false;// 消息是否被预处理过
        String src = null;
        String payload;
        long origLength;
        if (bytes.length > 6 && bytes[0] == GMQTT_MAGIC_HEAD0 && bytes[1] == GMQTT_MAGIC_HEAD1 && (origLength = bigEndianBytesToUInt32(bytes, 2)) > 0 && origLength < bytes.length) {
            preProcessed = true;
            int size = (int) origLength;
            src = new String(bytes, 6, size);
            String after = src;
            int left = bytes.length - (6 + size);
            if (left > 0) {
                after = new String(bytes, 6 + size, left);
            }
            if (after.startsWith("ERROR:")) {// 预处理返回报错信息
                if (after.length() > 8 && after.charAt(6) == '{' && after.charAt(after.length() - 1) == '}') {
                    // field error
                    String errMsg = after.substring(6);
                    FieldErrMsg errorField = JsonUtil.fromJson(errMsg, FieldErrMsg.class);
                    if (errorField.getCode() == 2) {
                        errMsg = I18nUtils.getMessage("uns.invalid.toLong", errorField.getField());
                    } else {
                        errMsg = I18nUtils.getMessage("uns.invalid.type", errorField.getField());
                    }
                    sendProcessedTopicMessage(nowInMills, definition, src, null, errMsg);
                } else {
                    sendErrMsg(unsId, topic, after, nowInMills, definition, src);// json error
                }
                return;
            }
            payload = after;
        } else {
            payload = new String(bytes, StandardCharsets.UTF_8);
        }
        lastMsg.update(topic, msgId, payload); // update metric
        Object vo;
        try {
            vo = JsonUtil.fromJson(payload);
        } catch (Exception ex) {
            sendErrMsg(unsId, topic, payload, nowInMills, definition, src);
            return;
        }
        List<Map<String, Object>> list;
        if (preProcessed) {// 预处理过的不再校验格式
            char c0 = payload.charAt(0);
            if (c0 == '{') {
                list = Collections.singletonList((Map) vo);
            } else if (c0 == '[') {
                list = (List<Map<String, Object>>) vo;
            } else {
                list = Collections.emptyList();
            }
        } else {
            if (payload.charAt(0) == '{' && vo instanceof Map) {
                Map map = (Map) vo;
                Object raw = map.get(Constants.MSG_RAW_DATA_KEY);
                if (raw != null) {
                    src = JSON.toJSONString(raw);
                    if ("".equals(src)) {
                        src = "{}";
                    }
                }
                Object res = map.get(Constants.MSG_RES_DATA_KEY);
                if (res != null) {
                    vo = res;
                }
            }
            FindDataListUtils.SearchResult rs = FindDataListUtils.findDataList(vo, 1, fieldDefines);
            list = rs.list;
            log.debug("onMsg[{}]: list[{}]={}, src={}, payload={}", topic, list != null ? list.size() : -1, list, src, payload);
            if (src == null) {
                src = payload;
            }
            if (list == null || list.isEmpty() || rs.errorField != null || rs.toLongField != null) {
                TopologyLog.log(unsId, TopologyLog.Node.PULL_MQTT, TopologyLog.EventCode.ERROR, I18nUtils.getMessage("uns.topology.mqtt.parse"));
                log.warn("DataListNotFound[{}] : payload = {}", topic, payload);
                String err = null;
                Long qos = null;
                String fieldName = null;
                if (rs.errorField != null) {
                    qos = 0x400000000000000L;//类型转换失败
                    fieldName = rs.errorField;
                    err = I18nUtils.getMessage("uns.invalid.type", rs.errorField);
                }
                if (rs.toLongField != null) {
                    qos = 0x80000000000000L;//超量程（工程单位）值"
                    fieldName = rs.toLongField;
                    String tip = I18nUtils.getMessage("uns.invalid.toLong", rs.toLongField);
                    err = err != null ? err + "; " + tip : tip;
                }
                sendProcessedTopicMessage(nowInMills, definition, src, null, err);
                if (qos != null) {
                    final String CT = definition.getCreateTopicDto().getTimestampField();
                    final String qosField = definition.getCreateTopicDto().getQualityField();
                    FieldDefine define = definition.getFieldDefines().getFieldsMap().get(fieldName);
                    LinkedHashMap<String, Object> obj = new LinkedHashMap<>(4);
                    if (vo instanceof Map<?, ?> vmap) {
                        obj.put(CT, vmap.get(CT));
                    }
                    obj.put(fieldName, define != null ? define.getType().defaultValue : "0");
                    obj.put(qosField, qos);
                    list = Collections.singletonList(obj);
                } else {
                    return;
                }
            }
        }
        list = mergeBeansWithTimestamp(list, definition, nowInMills);
        final long msgTimeMills;
        Long lastBeanTime = definition.getLastDateTime();
        if (lastBeanTime != null) {
            msgTimeMills = DateTimeUtils.convertToMills(lastBeanTime);
        } else {
            msgTimeMills = nowInMills;
        }
        sendProcessedTopicMessage(msgTimeMills, definition, src, list, null);
        if (definition.getDataType() == Constants.ALARM_RULE_TYPE) {
            return;
        }
        queueLocker.readLock().lock();
        try {
            queue.enqueue(JsonUtil.toJsonBytes(new TopicMessage(definition.getCreateTopicDto().getId(), list)));
            enqueuedSize.incrementAndGet();
            semaphore.release();
        } finally {
            queueLocker.readLock().unlock();
        }
    }

    private void sendErrMsg(Long unsId, String topic, String payload, long nowInMills, TopicDefinition definition, String src) {
        TopologyLog.log(unsId, TopologyLog.Node.PULL_MQTT, TopologyLog.EventCode.ERROR, I18nUtils.getMessage("uns.topology.mqtt.parse"));
        log.warn("bad JSON[{}] : payload = {}, src={}", topic, payload, src);
        sendProcessedTopicMessage(nowInMills, definition, src, null, I18nUtils.getMessage("uns.invalid.json"));
    }

    private void sendProcessedTopicMessage(final long nowInMills, TopicDefinition definition, String rawData, List<Map<String, Object>> dataToSend, String errMsg) {
        String topic = definition.getTopic();
        CreateTopicDto info = definition.getCreateTopicDto();
        Integer dataType = info.getDataType();
        TopicMessageEvent event = new TopicMessageEvent(this, info, info.getId(), dataType != null ? dataType : -1, definition.getFieldDefines().getFieldsMap(), topic, info.getProtocolType(), dataToSend != null ? dataToSend.get(dataToSend.size() - 1) : null, definition.getLastMsg(), definition.getLastDt(), rawData, nowInMills, errMsg);
        dataPublishExecutor.submit(() -> {
            this.topicSender.submit(() -> EventBus.publishEvent(event));
        });
    }

    private static List<Map<String, Object>> mergeBeansWithTimestamp(List<Map<String, Object>> list, TopicDefinition definition, long nowInMills) {
        Long prevTime = null;
        ConcurrentHashMap<String, Object> lastMsg = definition.getLastMsg();
        ConcurrentHashMap<String, Long> dtMap = definition.getLastDt();
        Map<String, Object> prevBean = new HashMap<>();
        final String CT = definition.getCreateTopicDto().getTimestampField();
        if (lastMsg != null) {
            Object lastTime = lastMsg.get(CT);
            if (lastTime instanceof Long) {
                prevTime = (Long) lastTime;
                final long pt = prevTime;
                for (Map.Entry<String, Long> dt : dtMap.entrySet()) {
                    String k = dt.getKey();
                    if (dt.getValue() == pt) {
                        prevBean.put(k, lastMsg.get(k));
                    }
                }
            }
        } else {
            definition.setLastMsg(lastMsg = new ConcurrentHashMap<>());
            definition.setLastDt(dtMap = new ConcurrentHashMap<>());
        }
        final boolean firstMsg = lastMsg.isEmpty();
        final Long lastUpdateTime = definition.getLastDateTime();
        final ArrayList<Map<String, Object>> mergedList = new ArrayList<>(list.size());
        final boolean mergeTime = definition.getJdbcType().typeCode == Constants.TIME_SEQUENCE_TYPE;// 时序数据都按时间戳合并
        final String qos = definition.getCreateTopicDto().getQualityField();
        for (Map<String, Object> bean : list) {
            Object lastBeanTime = bean.computeIfAbsent(CT, k -> nowInMills);
            if (lastBeanTime instanceof String) {
                try {
                    TemporalAccessor tm = ZonedDateTime.parse(lastBeanTime.toString());
                    lastBeanTime = Instant.from(tm).toEpochMilli();
                    bean.put(CT, lastBeanTime);
                } catch (Exception ex) {
                    log.debug("DateTimeFormatError: {}", bean, ex);
                }
            }
            if (qos != null) {
                bean.putIfAbsent(qos, 0);// 写入质量码默认值：Good(0)
            }
            if (!(lastBeanTime instanceof Long)) {
                bean.put(CT, lastBeanTime = nowInMills);
            }
            Long curTime = (Long) lastBeanTime;
            if (mergeTime && prevTime != null && curTime.longValue() == prevTime.longValue()) {
                if (!mergedList.isEmpty()) {
                    Map<String, Object> last = mergedList.get(mergedList.size() - 1);
                    Map<String, Object> mm = new HashMap<>(last);
                    mm.putAll(bean);
                    mm.put(Constants.MERGE_FLAG, 1);
                    mergedList.set(mergedList.size() - 1, mm);
                } else {
                    Map<String, Object> mm = new HashMap<>(prevBean);
                    mm.putAll(bean);
                    mm.put(Constants.MERGE_FLAG, 1);
                    mergedList.add(mm);
                }
            } else {
                mergedList.add(bean);
            }
            prevTime = curTime;
            prevBean = bean;
            for (Map.Entry<String, Object> entry : bean.entrySet()) {
                String k = entry.getKey();
                if (Character.isJavaIdentifierStart(k.charAt(0))) {
                    Object v = entry.getValue();
                    if (v != null) {
                        lastMsg.put(k, v);
                        dtMap.put(k, curTime);
                    }
                }
            }
            if (firstMsg) {
                bean.put(Constants.FIRST_MSG_FLAG, 1);
            }
            definition.setLastDateTime(curTime);
        }
        log.debug("merge: {} -> {}", list, mergedList);

        Map<String, FieldDefine> fieldsMap = definition.getFieldDefines().getFieldsMap();
        if (lastMsg.size() > fieldsMap.size() || (lastUpdateTime == null || nowInMills - lastUpdateTime > 5000)) {
            ConcurrentHashMap<String, Long> dt = dtMap;
            lastMsg.keySet().removeIf(field -> {
                boolean invalid = !fieldsMap.containsKey(field);
                if (invalid) {
                    dt.remove(field);
                }
                return invalid;
            });
        }
        return mergedList;
    }

    static Map<Long, Object[]> tryCalc(Map<Long, TopicDefinition> topicDefinitionMap, TopicDefinition calc, SaveDataDto cur, HashMap<Long, SaveDataDto> topicData, AtomicInteger count) {
        if (calc == null) {
            log.debug("TopicDefinitionNotFound: id={}", cur.getId());
            return null;
        }
        final int CUR_SIZE = cur.getList().size();
        String calcAlias = calc.getCreateTopicDto().getAlias();
        FieldDefine calcField = calc.getFieldDefines().getCalcField();
        log.debug("tryCalc: {} when proc:{}, list[{}] = {}", calcAlias, cur.getId(), CUR_SIZE, cur.getList());
        Object expr = calc.getCompileExpression();
        if (expr == null) {
            log.debug("CompileExpressionNull when alias={}", calcAlias);
            return null;
        }
        if (calcField == null) {
            log.debug("calcFieldNotFound when alias={}", calcAlias);
            return null;
        }
        LinkedHashMap<Long, Object[]> rsMap = new LinkedHashMap<>();
        for (int K = 0; K < CUR_SIZE; K++) {
            InstanceField[] refers = calc.getRefers();
            Map<String, Object> vars = Collections.emptyMap();
            long maxTime = -1;
            long baseTime = 0;// 基准时间优先使用，其次是 maxTime
            Long qos = null;
            if (ArrayUtil.isNotEmpty(refers)) {
                vars = new HashMap<>(Math.max(refers.length, 8));
                boolean next = true;
                for (int i = 0; i < refers.length; i++) {
                    InstanceField field = refers[i];
                    if (field != null && field.getField() != null) {
                        AtomicLong qosHolder = new AtomicLong();
                        Long timestamp = fillVars(topicDefinitionMap, topicData, vars, i, field, qosHolder);
                        if (timestamp != null) {
                            if (Boolean.TRUE.equals(field.getUts())) {
                                if (baseTime < 1) {// 只有首个基准时间生效
                                    baseTime = timestamp;
                                    qos = qosHolder.get();
                                }
                            } else if (baseTime < 1 && timestamp.longValue() > maxTime) {
                                maxTime = timestamp.longValue();
                                long curQos = qosHolder.get();
                                if (curQos != 0L) {
                                    qos = curQos;
                                }
                            }
                        } else {
                            log.debug("引用还没有值： {}.{}, calcAlias={}", calcAlias, field, calcAlias);
                            next = false;
                            break;
                        }
                    }
                }
                if (!next) {
                    clearState(topicData);
                    log.debug("No timestamp for calcAlias: {} {}", calcAlias, K);
                    return null;
                }
            }

            Object evalRs = null;
            try {
                evalRs = ExpressionFunctions.executeExpression(expr, vars);
            } catch (Exception ex) {
                if (baseTime < 1) {
                    qos = 0x400000000000000L;//表达式计算失败，覆盖原始质量码
                }
            }
            if (baseTime < 1 && qos != null) {
                qos = 0x400000000000000L;
            }
            if (evalRs != null) {
                FieldType fieldType = calcField.getType();
                if (evalRs instanceof Number) {
                    Number num = (Number) evalRs;
                    if (fieldType == FieldType.BOOLEAN) {
                        evalRs = num.longValue() != 0;
                    } else {
                        switch (fieldType) {
                            case INTEGER:
                            case LONG:
                                evalRs = num.longValue();
                                break;
                        }
                    }
                } else if (evalRs instanceof Boolean) {
                    if (fieldType.isNumber) {
                        Boolean v = (Boolean) evalRs;
                        evalRs = v ? 1 : 0;
                    }
                }
            }
            if (evalRs == null) {
                evalRs = 0;
            }
            rsMap.put(baseTime > 0 ? baseTime : maxTime, new Object[]{evalRs, vars, qos});

            count.incrementAndGet();
        }
        clearState(topicData);
        return rsMap;
    }

    private static void clearState(HashMap<Long, SaveDataDto> topicData) {
        for (SaveDataDto dto : topicData.values()) {
            dto.setListItr(null);
        }
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(100)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        if (!CollectionUtils.isEmpty(event.topics)) {
            for (Long id : event.topics.keySet()) {
                TopicDefinition definition = topicDefinitionMap.remove(id);
                if (definition != null) {
                    int dataType = definition.getDataType();
                    if (dataType == Constants.MERGE_TYPE) {
                        scheduleCalcTopics.remove(id);
                    } else if (dataType == Constants.CITING_TYPE) {
                        InstanceField[] refers = definition.getRefers();
                        if (refers != null) {
                            for (InstanceField file : refers) {
                                TopicDefinition def = topicDefinitionMap.get(file.getId());
                                if (def != null) {
                                    def.getCreateTopicDto().getCited().remove(id);
                                }
                            }
                        }
                    }
                    CreateTopicDto old = definition.getCreateTopicDto();
                    aliasMap.remove(old.getAlias(), id);
                    pathMap.remove(old.getPath(), id);
                }
            }
            reCalculateRefers();
        }
    }

    private static Long fillVars(Map<Long, TopicDefinition> topicDefinitionMap, HashMap<Long, SaveDataDto> topicData, Map<String, Object> vars, int i, InstanceField field, AtomicLong qosHolder) {
        Long topic = field.getId();
        String f = field.getField();
        TopicDefinition ref = topicDefinitionMap.get(topic);
        if (ref == null) {
            log.warn("未知Topic: {}", topic);
            return null;
        }

        SaveDataDto dto = topicData.get(topic);
        Iterator<Map<String, Object>> listItr = null;
        if (dto != null) {
            listItr = dto.getListItr();
            if (listItr == null && !CollectionUtils.isEmpty(dto.getList())) {
                dto.setListItr(listItr = dto.getList().iterator());
            }
        }
        Map<String, Object> lastMsg = ref.getLastMsg();
        if (lastMsg == null) {
            lastMsg = Collections.emptyMap();
        }
        Map<String, Object> msg = lastMsg;
        if (listItr != null && listItr.hasNext()) {
            msg = listItr.next();
            if (msg == null) {
                msg = Collections.emptyMap();
            }
        }
        Object v;
        Long timestamp = null;
        FieldDefine fieldDefine = ref.getFieldDefines().getFieldsMap().get(f);
        if (fieldDefine != null && ((v = msg.get(f)) != null || (v = lastMsg.get(f)) != null)) {
            Long time = (Long) msg.get(ref.getCreateTopicDto().getTimestampField());
            if (time != null) {
                timestamp = time;
            }
            String qosField = ref.getCreateTopicDto().getQualityField();
            if (qosField != null) {
                Object qosObj = msg.get(qosField);
                if (qosObj != null) {
                    if (qosObj instanceof Number qos) {
                        qosHolder.set(qos.longValue());
                    } else {
                        try {
                            Double dv = Double.parseDouble(qosObj.toString().trim());
                            qosHolder.set(dv.longValue());
                        } catch (NumberFormatException nfe) {
                        }
                    }
                }
            }
            FieldType fieldType = fieldDefine.getType();
            if (fieldType.isNumber) {
                if (fieldType == FieldType.INTEGER) {
                    v = IntegerUtils.parseInt(v.toString());
                } else {
                    try {
                        Double d = Double.parseDouble(v.toString());
                        v = d;
                        if (fieldType == FieldType.LONG) {
                            v = d.longValue();
                        }
                    } catch (Exception ex) {
                        log.debug("{}.{} IsNaN: {}, When Double", v, topic, f);
                    }
                }
                if (v == null) {
                    v = 0;
                }
            } else if (fieldType == FieldType.BOOLEAN && !(v instanceof Boolean)) {
                Integer num = IntegerUtils.parseInt(v.toString());
                if (num != null) {
                    v = num != 0;
                } else {
                    try {
                        double dv = Double.parseDouble(v.toString());
                        v = dv > 0;
                    } catch (Exception ex) {
                        log.debug("{}.{} IsNaN: {}, when Boolean", v, topic, f);
                        v = false;
                    }
                }
            }
            vars.put(Constants.VAR_PREV + (i + 1), v);
        }
        return timestamp;
    }

    @EventListener(classes = BatchCreateTableEvent.class)
    @Order(1900)
    @Description("uns.create.task.name.mqtt")
    void onBatchCreateTable(BatchCreateTableEvent event) {
        if (!ArrayUtil.isEmpty(event.topics)) {
            final boolean sub = !subscribeALL;
            if (sub) {
                TreeSet<String> topLevels = new TreeSet<>();
                for (CreateTopicDto[] dtoArray : event.topics.values()) {
                    for (CreateTopicDto dto : dtoArray) {
                        String prev = parseTopLevel(dto.getTopic());
                        topLevels.add(prev);
                        addTopicFields(dto);
                    }
                }
                log.info("+订阅：{}", topLevels);
                mqttPublisher.subscribe(topLevels, false);
            } else {
                for (CreateTopicDto[] dtoArray : event.topics.values()) {
                    for (CreateTopicDto dto : dtoArray) {
                        addTopicFields(dto);
                    }
                }
            }
            reCalculateRefers();
        }
    }

    @EventListener(classes = InitTopicsEvent.class)
    @Order(99)
    void onInitTopicsEvent(InitTopicsEvent event) {
        if (!CollectionUtils.isEmpty(event.topics)) {
            TreeSet<String> topLevels = new TreeSet<>();
            for (List<CreateTopicDto> list : event.topics.values()) {
                for (CreateTopicDto dto : list) {
                    String prev = parseTopLevel(dto.getTopic());
                    topLevels.add(prev);
                    addTopicFields(dto);
                }
            }
            reCalculateRefers();
            log.debug("初始订阅：{}", topLevels);
            if (!subscribeALL) {
                mqttPublisher.subscribe(topLevels, false);
            }
        }
    }

    private static String parseTopLevel(String topic) {
        int x = topic.indexOf('/', 1);
        if (x > 0) {
            return topic.substring(0, x + 1) + "#";
        } else {
            return topic;
        }
    }

    @EventListener(classes = UpdateInstanceEvent.class)
    @Order(99)
    void onUpdateInstanceEvent(UpdateInstanceEvent event) {
        if (!CollectionUtils.isEmpty(event.topics)) {
            for (CreateTopicDto dto : event.topics) {
                addTopicFields(dto);
            }
            reCalculateRefers();
        }
    }

    private void reCalculateRefers() {
        for (TopicDefinition definition : topicDefinitionMap.values()) {
            InstanceField[] refers = definition.getRefers();
            Set<Long> refTopics;
            if (ArrayUtil.isNotEmpty(refers)) {
                Long calcTopic = definition.getCreateTopicDto().getId();
                for (InstanceField field : refers) {
                    if (field != null && field.getId() != null) {
                        TopicDefinition def = topicDefinitionMap.get(field.getId());
                        if (def != null) {
                            if (definition.getDataType() == Constants.CITING_TYPE) {
                                def.getCreateTopicDto().getCited().add(calcTopic);// 标记为被引用类型文件所引用
                            } else {
                                def.addReferCalcTopic(calcTopic);
                            }
                        }
                    }
                }
            } else if ((refTopics = definition.getReferCalcUns()) != null) {
                Iterator<Long> refItr = refTopics.iterator();
                while (refItr.hasNext()) {
                    Long refTopic = refItr.next();
                    if (!topicDefinitionMap.containsKey(refTopic)) {
                        refItr.remove();// remove invalid reference
                    }
                }
                if (refTopics.isEmpty()) {
                    definition.setReferCalcUns(null);
                }
            }
        }
    }

    void addTopicFields(CreateTopicDto dto) {
        addTopicFields(topicDefinitionMap, dto);
        addScheduleCalcTask(dto);
    }

    private void addScheduleCalcTask(CreateTopicDto dto) {
        if (dto.getDataType() == Constants.MERGE_TYPE) {
            Long id = dto.getId();
            if (scheduleCalcTopics.add(id)) {
                tryMergeTopics(id);
            }
        }
    }

    private void tryMergeTopics(Long id) {
        TopicDefinition definition = topicDefinitionMap.get(id);
        CreateTopicDto dto = definition != null ? definition.getCreateTopicDto() : null;
        Long freq;
        if (dto != null && dto.getDataType() == Constants.MERGE_TYPE && (freq = dto.getFrequencySeconds()) != null && freq > 0) {
            systemTimer.addTask(new TimerTask(new Runnable() {
                @Override
                public void run() {
                    mergeReferTopics(dto);
                    tryMergeTopics(id);
                }
            }, freq * 1000));
        } else {
            log.warn("不是合并实例: {}, dto={}", id, dto);
        }
    }

    private void mergeReferTopics(CreateTopicDto dto) {
        StringBuilder sb = new StringBuilder(256);
        for (InstanceField field : dto.getRefers()) {
            Long refId = field.getId();
            TopicDefinition definition = topicDefinitionMap.get(refId);
            Map<String, Object> lastMsg = definition.getLastMsg();
            if (lastMsg != null) {
                if (sb.length() > 0) {
                    sb.append(',');
                } else {
                    TopicDefinition mdf = topicDefinitionMap.get(dto.getId());
                    FieldDefines defines = mdf.getFieldDefines();
                    sb.append("{\"").append(defines.getCalcField().getName()).append("\":\"{");
                }
                sb.append("\\\"").append(definition.getTable()).append("\\\":");
                add2Json(definition.getFieldDefines(), lastMsg, sb);
            }
        }
        final String mergeTopic = dto.getTopic();
        if (sb.length() == 0) {
            log.trace("NO合并值：{}", mergeTopic);
            return;
        }
        sb.append("}\"}");
        final String dataJson = sb.toString();
        log.debug("发送合并值：{}: {}", mergeTopic, dataJson);
        try {
            mqttPublisher.publishMessage(mergeTopic, dataJson.getBytes(StandardCharsets.UTF_8), 0);
            publishedMergeSize.incrementAndGet();
        } catch (MqttException e) {
            log.error("ErrPublishMergeMsg: topic={} , data={}", mergeTopic, dataJson, e);
        }

    }

    static void add2Json(FieldDefines fieldDefines, Map<String, Object> bean, StringBuilder sb) {
        sb.append('{');
        final int len = sb.length();
        Map<String, FieldDefine> fieldDefineMap = fieldDefines.getFieldsMap();
        for (Map.Entry<String, Object> entry : bean.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith(Constants.SYSTEM_FIELD_PREV)) {
                continue;
            }
            Object v = entry.getValue();
            FieldDefine fieldDefine = fieldDefineMap.get(name);
            if (fieldDefine != null && v != null) {
                sb.append("\\\"").append(name).append("\\\":");
                FieldType fieldType = fieldDefine.getType();
                boolean isZ = fieldType == FieldType.INTEGER || fieldType == FieldType.LONG;
                if (Number.class.isAssignableFrom(v.getClass())) {
                    sb.append(isZ ? ((Number) v).longValue() : v).append(',');
                } else {
                    String s = v.toString().replace("\"", "\\\\\\\"");
                    s = s.replace("'", "\\u0027");
                    sb.append("\\\"").append(s).append("\\\",");
                }
            }
        }
        if (sb.length() > len) {
            sb.setCharAt(sb.length() - 1, '}');
        } else {
            sb.append('}');
        }
    }

    void addTopicFields(Map<Long, TopicDefinition> topicDefinitionMap, CreateTopicDto dto) {
        final Long id = dto.getId();
        TopicDefinition prevDefinition = topicDefinitionMap.get(id);
        String a = dto.getAlias(), p = dto.getPath();
        Long oldAId = aliasMap.put(a, id);
        Long oldPId = pathMap.put(p, id);
        if (prevDefinition != null) {
            CreateTopicDto old = prevDefinition.getCreateTopicDto();
            String a1 = old.getAlias(), p1 = old.getPath();
            if (!Objects.equals(a, a1) && id.equals(oldAId)) {
                aliasMap.remove(a1);
            }
            if (!Objects.equals(p, p1) && id.equals(oldPId)) {
                pathMap.remove(p1);
            }
            prevDefinition.setCreateTopicDto(dto);
        } else {
            topicDefinitionMap.put(id, new TopicDefinition(dto));
        }
    }

    @Override
    public void onMessageByAlias(String alias, String payload) {
        if (alias != null) {
            Long id = aliasMap.get(alias);
            TopicDefinition definition = id != null ? topicDefinitionMap.get(id) : null;
            if (definition != null && definition.getDataType() == Constants.CITING_TYPE) {
                log.warn("拒绝引用类型写值: alias={}", alias);
                return;
            }
            onMessage(definition, alias, -1, payload.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public CreateTopicDto getDefinitionByAlias(String alias) {
        if (alias != null) {
            Long id = aliasMap.get(alias);
            return getUnsById(id);
        } else {
            return null;
        }
    }

    @Override
    public CreateTopicDto getDefinitionByPath(String path) {
        if (path != null) {
            Long id = pathMap.get(path);
            return getUnsById(id);
        } else {
            return null;
        }
    }

    public CreateTopicDto getDefinitionById(Long id) {
        return getUnsById(id);
    }

    private CreateTopicDto getUnsById(Long id) {
        if (id != null) {
            TopicDefinition definition = topicDefinitionMap.get(id);
            if (definition != null) {
                return definition.getCreateTopicDto();
            }
        }
        return null;
    }

    private final MetricRegistry metrics = new MetricRegistry();
    private final Counter requestCounter = metrics.counter("requests");
    final Histogram throughputHistogram = metrics.histogram(name(getClass(), "result-counts"));

    private final ScheduledExecutorService metricsExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        AtomicInteger threadNum = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "sinkMetric-" + threadNum.incrementAndGet());
        }
    });

    {
        AtomicLong last = new AtomicLong();
        metricsExecutorService.scheduleAtFixedRate(() -> {
            long count = requestCounter.getCount();
            long requestsInLastSecond = count - last.get();
            last.set(count);
            throughputHistogram.update(requestsInLastSecond);
        }, 1, 1, TimeUnit.SECONDS);
    }


    public double[] statisticsThroughput() {
        Snapshot snapshot = throughputHistogram.getSnapshot();
        return new double[]{snapshot.getMin(), snapshot.getMax(), snapshot.get75thPercentile(), snapshot.get95thPercentile(), snapshot.get999thPercentile()};
    }
}
