package com.supos.adapter.mqtt.webapi;

import com.alibaba.fastjson2.JSONObject;
import com.supos.adapter.mqtt.adapter.MQTTConsumerAdapter;
import com.supos.adapter.mqtt.dto.TopicDefinition;
import com.supos.adapter.mqtt.service.MessageConsumer;
import com.supos.adapter.mqtt.service.impl.UnsMessageConsumer;
import com.supos.adapter.mqtt.util.DateUtil;
import com.supos.common.dto.BaseResult;
import com.supos.common.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class MQTTMetricsController {
    @Autowired(required = false)
    MQTTConsumerAdapter mqttAdapter;
    @Autowired
    MessageConsumer messageConsumer;
    String startTime = DateUtil.dateStr(System.currentTimeMillis());


    @GetMapping(value = "/inter-api/supos/mqtt/metrics", produces = "application/json")
    public String metrics(@RequestParam(name = "f", required = false) Integer fetchSize,
                          @RequestParam(name = "w", required = false) Integer maxWaitMills,
                          @RequestParam(name = "env", required = false) Boolean showEnv,
                          @RequestParam(name = "sys", required = false) Boolean showSys
    ) {
        JSONObject json = new JSONObject();
        if (mqttAdapter != null) {
            json.put("clientId", mqttAdapter.getClientId());
        }
        json.put("throughput", messageConsumer.statisticsThroughput());
        json.put("queueHead", messageConsumer.getQueueHead());
        json.put("queueHeadIndex", messageConsumer.getQueueFrontIndex());
        json.put("queueTailIndex", messageConsumer.getQueueTailIndex());
        json.put("queueSize", messageConsumer.getQueueSize());
        if (mqttAdapter != null) {
            json.put("arrivedSize", mqttAdapter.getArrivedSize());
        }
        json.put("queue.inp", messageConsumer.getEnqueuedSize());
        json.put("queue.out", messageConsumer.getDequeuedSize());
        json.put("calc.published", messageConsumer.getPublishedCalcSize());
        json.put("calc.arrived", messageConsumer.getArrivedCalcSize());
        json.put("merged.published", messageConsumer.getPublishedMergedSize());
        json.put("lastMsg", messageConsumer.getLastMessage());
        if (mqttAdapter != null) {
            json.put("connectLoss", mqttAdapter.getConnectionLossRecord());
        }
        json.put("startTime", startTime);
        json.put("fetchSize", UnsMessageConsumer.FETCH_SIZE);
        json.put("maxWaitMills", UnsMessageConsumer.MAX_WAIT_MILLS);
        if (showEnv != null && showEnv) {
            json.put("System_env", System.getenv());
        }
        if (showSys != null && showSys) {
            json.put("System_properties", System.getProperties());
        }
        if (fetchSize != null && fetchSize > 0) {
            UnsMessageConsumer.FETCH_SIZE = fetchSize;
        }
        if (maxWaitMills != null && maxWaitMills > 0) {
            UnsMessageConsumer.MAX_WAIT_MILLS = maxWaitMills;
        }
        return json.toJSONString();
    }

    @GetMapping(value = "/inter-api/supos/mqtt/topics", produces = "application/json")
    public String topicDefinitions(@RequestParam(name = "t", required = false) Long id,
                                   @RequestParam(name = "k", required = false) String key
    ) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("startTime", startTime);
        Map<Long, TopicDefinition> map = messageConsumer.getTopicDefinitionMap();
        if (id != null) {
            TopicDefinition definition = map.get(id);
            json.put("definition", definition);
        } else {
            if (StringUtils.hasText(key)) {
                List<String> topicTableMap = map.values().stream()
                        .filter(t -> t.getTopic().contains(key))
                        .map(t -> t.getTopic() + " : " + t.getTable()).collect(Collectors.toList());
                json.put("topicTables", topicTableMap);
            }
            if (mqttAdapter != null) {
                Collection<String> subscribeTopics = mqttAdapter.getSubscribeTopics();
                json.put("subscribes", subscribeTopics);
                if (subscribeTopics.size() < 2) {
                    json.put("topics", new TreeSet<>(map.keySet()));
                }
            }
        }
        return JsonUtil.toJsonUseFields(json);
    }

    @GetMapping(value = "/inter-api/supos/mqtt/rec", produces = "application/json")
    public BaseResult reconnect() throws Exception {
        try {
            mqttAdapter.reconnect();
            return new BaseResult(0, "ok");
        } catch (Exception ex) {
            return new BaseResult(500, ex.getMessage());
        }
    }
}
