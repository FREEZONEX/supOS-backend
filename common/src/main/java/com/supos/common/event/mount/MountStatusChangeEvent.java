package com.supos.common.event.mount;

import org.springframework.context.ApplicationEvent;

/**
 * @author sunlifang@supos.supcon.com
 * @title MountStatusEvent
 * @description
 * @create 2025/6/23 下午6:32
 */
public class MountStatusChangeEvent extends ApplicationEvent {


    public MountStatusChangeEvent(Object source) {
        super(source);
    }
}
