package com.supos.ws.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubscribeRequest extends ActionBaseRequest {

    private ActionData data;

    @Data
    public static class Source {
        private List<String> topic;
    }

    @Data
    public static class ActionData {
        private Source source;
    }
}
