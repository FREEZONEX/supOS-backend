package com.supos.common.dto;

import com.google.common.collect.Lists;
import com.supos.common.utils.JsonUtil;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 将拓扑日志保存至拓扑日志文件
 * @date 2024/12/24 14:38
 */
@Data
public class TopologyLog {

    public static List<String> topologyNodes = Lists.newArrayList(Node.PUSH_ORIGINAL_DATA, Node.PUSH_MQTT, Node.PULL_MQTT, Node.DATA_PERSISTENCE);

    private static final Logger LOG = LoggerFactory.getLogger("topology");

    private String unsId;
    private String topologyNode;
    private String eventCode;
    private String eventMessage;
    private Long eventTime;

    public static void log(Long unsId, String topologyNode, String eventCode, String eventMessage) {
        TopologyLog topologyLog = new TopologyLog();
        topologyLog.setUnsId(String.valueOf(unsId));
        topologyLog.setTopologyNode(topologyNode);
        topologyLog.setEventCode(eventCode);
        topologyLog.setEventMessage(eventMessage);
        topologyLog.setEventTime(System.currentTimeMillis());

        LOG.info(JsonUtil.toJson(topologyLog));
    }

    public static void log(Collection<Long> unsIds, String topologyNode, String eventCode, String eventMessage) {
        for (Long unsId : unsIds) {
            TopologyLog topologyLog = new TopologyLog();
            topologyLog.setUnsId(String.valueOf(unsId));
            topologyLog.setTopologyNode(topologyNode);
            topologyLog.setEventCode(eventCode);
            topologyLog.setEventMessage(eventMessage);
            topologyLog.setEventTime(System.currentTimeMillis());

            LOG.info(JsonUtil.toJson(topologyLog));
        }
    }

    public static void log(String topologyNode, String eventCode, String eventMessage) {
        TopologyLog topologyLog = new TopologyLog();
        topologyLog.setUnsId("_ALL");
        topologyLog.setTopologyNode(topologyNode);
        topologyLog.setEventCode(eventCode);
        topologyLog.setEventMessage(eventMessage);
        topologyLog.setEventTime(System.currentTimeMillis());

        LOG.info(JsonUtil.toJson(topologyLog));
    }

    public static class Node {
        public static final String PUSH_ORIGINAL_DATA = "pushOriginalData";
        public static final String PUSH_MQTT = "pushMqtt";
        public static final String PULL_MQTT = "pullMqtt";
        public static final String DATA_PERSISTENCE = "dataPersistence";
    }

    public static class EventCode {
        public static final String ERROR = "1";
        public static final String SUCCESS = "0";
    }
}
