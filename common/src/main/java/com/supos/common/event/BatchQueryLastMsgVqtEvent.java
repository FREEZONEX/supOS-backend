package com.supos.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.Map;

public class BatchQueryLastMsgVqtEvent extends ApplicationEvent {

    public final String tableName;

    public final List<Long> unsIds;


    public BatchQueryLastMsgVqtEvent(Object source, String tableName, List<Long> unsIds) {
        super(source);
        this.tableName = tableName;
        this.unsIds = unsIds;
    }

    @Getter
    @Setter
    List<Map<String, Object>> values;
}
