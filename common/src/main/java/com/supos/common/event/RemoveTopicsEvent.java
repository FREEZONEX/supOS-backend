package com.supos.common.event;

import com.supos.common.SrcJdbcType;
import com.supos.common.dto.SimpleUnsInstance;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;
import java.util.Map;

public class RemoveTopicsEvent extends ApplicationEvent {
    public final SrcJdbcType jdbcType;
    public final Map<Long, SimpleUnsInstance> topics;
    public final boolean withFlow;
    public final boolean withDashboard;
    public final Collection<String> modelTopics;

    public RemoveTopicsEvent(Object source, SrcJdbcType jdbcType, Map<Long, SimpleUnsInstance> topics, boolean withFlow, boolean withDashboard, Collection<String> modelTopics) {
        super(source);
        this.jdbcType = jdbcType;
        this.topics = topics;
        this.withFlow = withFlow;
        this.withDashboard = withDashboard;
        this.modelTopics = modelTopics;
    }
}
