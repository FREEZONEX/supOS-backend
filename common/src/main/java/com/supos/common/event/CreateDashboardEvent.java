package com.supos.common.event;

import com.supos.common.utils.JsonUtil;
import org.springframework.context.ApplicationEvent;

/**
 * 创建grafana dashboard 数据库记录
 */
public class CreateDashboardEvent extends ApplicationEvent {
    public final String uuid;
    /**
     * this name = uns alias
     */
    public final String name;
    public final String description;
    public final String username;

    public CreateDashboardEvent(Object source, String uuid, String name, String description, String username) {
        super(source);
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.username = username;
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
