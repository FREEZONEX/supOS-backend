package com.supos.ws.dto;

import lombok.Data;

import java.util.List;

@Data
public class PublishRequest extends ActionBaseRequest {

    private ActionData data;

    @Data
    public static class Payload {

        private String topic;

        private byte[] payload;
    }

    @Data
    public static class ActionData {
        //采集器别名
        private String source;

        private List<Payload> payload;

    }
}
