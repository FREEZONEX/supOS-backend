package com.supos.common.event;

import com.supos.common.dto.CreateTopicDto;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class UpdateInstanceEvent extends ApplicationEvent {
    public final List<CreateTopicDto> topics;
    public final CreateTopicDto[] folder;
    public final CreateTopicDto[] templates;

    public UpdateInstanceEvent(Object source, List<CreateTopicDto> topics) {
        this(source, topics, null, null);
    }
    public UpdateInstanceEvent(Object source, List<CreateTopicDto> topics,CreateTopicDto[] folder, CreateTopicDto[] templates) {
        super(source);
        this.topics = topics;
        this.folder = folder;
        this.templates = templates;
    }
}
