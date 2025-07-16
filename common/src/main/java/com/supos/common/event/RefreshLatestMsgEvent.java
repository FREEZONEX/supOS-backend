package com.supos.common.event;

import com.supos.common.SrcJdbcType;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class RefreshLatestMsgEvent extends ApplicationEvent {

    public Long unsId;

    public String path;

    public String payload;

    public Map<String, Long> dt;

    public Map<String, Object> data;

    public String errorMsg;

    public RefreshLatestMsgEvent(Object source, Long unsId, String path, String payload, Map<String, Long> dt, Map<String, Object> data, String errorMsg) {
        super(source);
        this.unsId = unsId;
        this.path = path;
        this.payload = payload;
        this.dt = dt;
        this.data = data;
        this.errorMsg = errorMsg;
    }
}
