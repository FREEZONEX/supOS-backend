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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@RestController
public class MQTTMetricsController {
    @Autowired
    MQTTConsumerAdapter mqttAdapter;
    @Autowired
    MessageConsumer messageConsumer;
    String startTime = DateUtil.dateStr(System.currentTimeMillis());

    @GetMapping(value = "/inter-api/supos/mqtt/metrics", produces = "application/json")
    public String metrics(@RequestParam(name = "f", required = false) Integer fetchSize,
                          @RequestParam(name = "w", required = false) Integer maxWaitMills,
                          @RequestParam(name = "sd", required = false) Boolean sendTopic,
                          @RequestParam(name = "env", required = false) Boolean showEnv,
                          @RequestParam(name = "sys", required = false) Boolean showSys
    ) {
        JSONObject json = new JSONObject();
        json.put("clientId", mqttAdapter.getClientId());
        json.put("throughput", mqttAdapter.statisticsThroughput());
        json.put("queueHead", messageConsumer.getQueueHead());
        json.put("queueHeadIndex", messageConsumer.getQueueFrontIndex());
        json.put("queueTailIndex", messageConsumer.getQueueTailIndex());
        json.put("queueSize", messageConsumer.getQueueSize());
        json.put("arrivedSize", mqttAdapter.getArrivedSize());
        json.put("queue.inp", messageConsumer.getEnqueuedSize());
        json.put("queue.out", messageConsumer.getDequeuedSize());
        json.put("calc.published", messageConsumer.getPublishedCalcSize());
        json.put("calc.arrived", messageConsumer.getArrivedCalcSize());
        json.put("merged.published", messageConsumer.getPublishedMergedSize());
        json.put("lastMsg", mqttAdapter.getLastMessage());
        json.put("connectLoss", mqttAdapter.getConnectionLossRecord());
        json.put("startTime", startTime);
        json.put("fetchSize", UnsMessageConsumer.FETCH_SIZE);
        json.put("maxWaitMills", UnsMessageConsumer.MAX_WAIT_MILLS);
        json.put("sendTopic", messageConsumer.isSendTopic());
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
        if (sendTopic != null) {
            messageConsumer.setSendTopic(sendTopic);
        }
        return json.toJSONString();
    }

    @GetMapping(value = "/inter-api/supos/mqtt/topics", produces = "application/json")
    public String topicDefinitions(@RequestParam(name = "t", required = false) String topic,
                                   @RequestParam(name = "k", required = false) String key
    ) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("startTime", startTime);
        if (StringUtils.hasText(topic)) {
            TopicDefinition definition = messageConsumer.getTopicDefinitionMap().get(topic);
            json.put("definition", definition);
        } else {
            if (StringUtils.hasText(key)) {
                List<String> topicTableMap = messageConsumer.getTopicDefinitionMap().values().stream()
                        .filter(t -> t.getTopic().contains(key))
                        .map(t -> t.getTopic() + " : " + t.getTable()).collect(Collectors.toList());
                json.put("topicTables", topicTableMap);
            }
            Collection<String> subscribeTopics = mqttAdapter.getSubscribeTopics();
            json.put("subscribes", subscribeTopics);
            if (subscribeTopics.size() < 2) {
                json.put("topics", new TreeSet<>(messageConsumer.getTopicDefinitionMap().keySet()));
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
