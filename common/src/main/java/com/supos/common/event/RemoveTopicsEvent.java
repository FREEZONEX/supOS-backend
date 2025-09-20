package com.supos.common.event;

import com.supos.common.dto.CreateTopicDto;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class RemoveTopicsEvent extends ApplicationEvent {
    public final Date deleteTime;
    public final boolean withFlow;
    public final boolean withDashboard;
    public final Collection<CreateTopicDto> topics;
    public final Collection<CreateTopicDto> templates;
    public final Collection<CreateTopicDto> folders;

    public RemoveTopicsEvent(Object source,
                             Date deleteTime,
                             boolean withFlow, boolean withDashboard,
                             Collection<CreateTopicDto> topics,
                             Collection<CreateTopicDto> templates,
                             Collection<CreateTopicDto> folders) {
        super(source);
        this.deleteTime = deleteTime;
        this.withFlow = withFlow;
        this.withDashboard = withDashboard;
        this.topics = topics != null ? topics : Collections.emptyList();
        this.templates = templates != null ? templates : Collections.emptyList();
        this.folders = folders != null ? folders : Collections.emptyList();
    }


}