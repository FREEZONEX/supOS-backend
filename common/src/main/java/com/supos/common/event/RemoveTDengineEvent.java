package com.supos.common.event;

import com.supos.common.SrcJdbcType;
import com.supos.common.dto.SimpleUnsInstance;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;
import java.util.Map;

public class RemoveTDengineEvent extends RemoveTopicsEvent {

    public RemoveTDengineEvent(Object source, SrcJdbcType jdbcType, Map<Long, SimpleUnsInstance> topics, boolean withFlow, boolean withDashboard, Collection<String> modelTopics) {
        super(source, jdbcType, topics, withFlow, withDashboard, modelTopics);
    }
}
