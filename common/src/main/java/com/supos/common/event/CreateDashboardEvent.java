package com.supos.common.event;

import com.supos.common.utils.JsonUtil;
import org.springframework.context.ApplicationEvent;

/**
 * 创建grafana dashboard 数据库记录
 */
public class CreateDashboardEvent extends ApplicationEvent {
    public final String uuid;
    public final String name;
    public final String description;

    public CreateDashboardEvent(Object source, String uuid, String name, String description) {
        super(source);
        this.uuid = uuid;
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
