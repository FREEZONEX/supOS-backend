package com.supos.i18n.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ResourceSignleRefreshEvent
 * @date 2025/9/6 10:58
 */
@Getter
public class ResourceSignleRefreshEvent extends ApplicationEvent {

    private String key;

    public ResourceSignleRefreshEvent(Object source, String key) {
        super(source);
        this.key = key;
    }
}
