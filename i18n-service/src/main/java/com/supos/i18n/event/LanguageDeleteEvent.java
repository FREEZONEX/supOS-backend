package com.supos.i18n.event;

import lombok.Data;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author sunlifang
 * @version 1.0
 * @description: LanguageDeleteEvent
 * @date 2025/9/6 10:58
 */
@Getter
public class LanguageDeleteEvent extends ApplicationEvent {

    private String languageCode;

    public LanguageDeleteEvent(Object source, String languageCode) {
        super(source);
        this.languageCode = languageCode;
    }
}
