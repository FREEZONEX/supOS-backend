package com.supos.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 采集器网关删除事件
 * @date 2025/4/10 15:21
 */
public class RemoveCollectorGatewayEvent extends ApplicationEvent {

    private String authUuid;

    public RemoveCollectorGatewayEvent(Object source, String authUuid) {
        super(source);
        this.authUuid = authUuid;
    }
}
