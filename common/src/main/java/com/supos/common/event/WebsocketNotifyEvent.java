package com.supos.common.event;

import org.springframework.context.ApplicationEvent;

public class WebsocketNotifyEvent extends ApplicationEvent {

    public Long unsId;

    public String path;

    public WebsocketNotifyEvent(Object source, Long unsId, String path) {
        super(source);
        this.unsId = unsId;
        this.path = path;
    }
}
