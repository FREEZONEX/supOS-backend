package com.supos.adapter.mqtt.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TopicMessage {
    Long id;
    Map<String, Object>[] msg;

    public TopicMessage() {
    }

    public TopicMessage(Long id, Map<String, Object>[] msg) {
        this.id = id;
        this.msg = msg;
    }
}
