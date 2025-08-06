package com.supos.common.event;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class RefreshLatestMsgEvent extends ApplicationEvent {

    public final Long unsId;
    public final Integer dataType;
    public final String path;

    public final String payload;

    public final Map<String, Long> dt;

    public final Map<String, Object> data;

    public final String errorMsg;

    public RefreshLatestMsgEvent(Object source, Long unsId, Integer dataType, String path, String payload, Map<String, Long> dt, Map<String, Object> data, String errorMsg) {
        super(source);
        this.unsId = unsId;
        this.dataType = dataType;
        this.path = path;
        this.payload = payload;
        this.dt = dt;
        this.data = data;
        this.errorMsg = errorMsg;
    }
}
