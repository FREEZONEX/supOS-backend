package com.supos.i18n.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ResourceRefreshEvent
 * @date 2025/9/6 10:58
 */
@Getter
public class ResourceRefreshEvent extends ApplicationEvent {

    private String languageCode;

    public ResourceRefreshEvent(Object source, String languageCode) {
        super(source);
        this.languageCode = languageCode;
    }
}
