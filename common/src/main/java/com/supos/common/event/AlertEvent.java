package com.supos.common.event;

import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class AlertEvent extends TopicMessageEvent {

    public AlertEvent(Object source, CreateTopicDto def, Long unsId, int dataType, Map<String, FieldDefine> fieldsMap, String topic, String protocol, Map<String, Object> data,
                             Map<String, Object> lastData, Map<String, Long> lastDataTime, String payload, long now, String err) {
        super(source, def, unsId, dataType, fieldsMap, topic, protocol, data, lastData, lastDataTime, payload, now, err);
    }
}
