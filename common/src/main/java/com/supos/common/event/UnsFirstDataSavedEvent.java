package com.supos.common.event;

import org.springframework.context.ApplicationEvent;

public class UnsFirstDataSavedEvent extends ApplicationEvent {
    public final Long unsId;
    public final Integer unsFlags;

    public UnsFirstDataSavedEvent(Object source, Long unsId, Integer unsFlags) {
        super(source);
        this.unsId = unsId;
        this.unsFlags = unsFlags;
    }
}
